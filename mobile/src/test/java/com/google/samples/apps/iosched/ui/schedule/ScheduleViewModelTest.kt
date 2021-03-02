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

package com.google.samples.apps.iosched.ui.schedule

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.androidtest.util.observeForTesting
import com.google.samples.apps.iosched.model.ConferenceData
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.model.TestDataSource
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.ConferenceDataSource
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfoBasic
import com.google.samples.apps.iosched.shared.data.signin.datasources.AuthStateUserDataSource
import com.google.samples.apps.iosched.shared.data.signin.datasources.RegisteredUserDataSource
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessage
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.domain.RefreshConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.ScheduleUiHintsShownUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadScheduleUserSessionsParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadScheduleUserSessionsResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadScheduleUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.ObserveConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.CoroutineScope
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakeAppDatabase
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeStarEventUseCase
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.signin.FirebaseSignInViewModelDelegate
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.verify

/**
 * Unit tests for the [ScheduleViewModel].
 */
class ScheduleViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val testDispatcher = coroutineRule.testDispatcher

    @Test
    fun testDataIsLoaded_ObservablesUpdated() = coroutineRule.runBlockingTest {
        // Create a delegate so we can load a user
        val signInDelegate = FakeSignInViewModelDelegate()

        // Create ViewModel with the use cases
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInDelegate)

        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser("test")

        // Observe viewmodel to load sessions
        viewModel.scheduleUiData.observeForTesting {
            // Check that data were loaded correctly
            assertEquals(
                TestData.userSessionList,
                LiveDataTestUtil.getValue(viewModel.scheduleUiData)?.list
            )
            assertFalse(LiveDataTestUtil.getValue(viewModel.isLoading)!!)
        }
    }

    @Test
    fun testDataIsLoaded_Fails() = coroutineRule.runBlockingTest {
        // Create ViewModel
        val viewModel = createScheduleViewModel(
            loadScheduleSessionsUseCase = createExceptionUseCase()
        )
        viewModel.errorMessage.observeForTesting {
            val errorMsg = LiveDataTestUtil.getValue(viewModel.errorMessage)
            assertTrue(errorMsg?.peekContent()?.isNotEmpty() ?: false)
        }
    }

    /** Starring **/

    @Test
    fun testStarEvent() {
        // Create test use cases with test data
        val snackbarMessageManager = SnackbarMessageManager(FakePreferenceStorage())
        val viewModel = createScheduleViewModel(snackbarMessageManager = snackbarMessageManager)

        viewModel.onStarClicked(TestData.userSession0)

        val nextMessageEvent: Event<SnackbarMessage>? =
            LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        val message = nextMessageEvent?.getContentIfNotHandled()
        assertThat(message?.messageId, `is`(equalTo(R.string.event_starred)))
        assertThat(message?.actionId, `is`(equalTo(R.string.dont_show)))

        // TODO: check changes in data source
    }

    @Test
    fun testUnstarEvent() {
        // Create test use cases with test data
        val snackbarMessageManager = SnackbarMessageManager(FakePreferenceStorage())
        val viewModel = createScheduleViewModel(snackbarMessageManager = snackbarMessageManager)

        viewModel.onStarClicked(TestData.userSession1)

        val nextMessageEvent: Event<SnackbarMessage>? =
            LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        val message = nextMessageEvent?.getContentIfNotHandled()
        assertThat(message?.messageId, `is`(equalTo(R.string.event_unstarred)))
        assertThat(message?.actionId, `is`(equalTo(R.string.dont_show)))
    }

    @Test
    fun testStar_notLoggedInUser() {
        // Create test use cases with test data
        val signInDelegate = FakeSignInViewModelDelegate()
        signInDelegate.injectIsSignedIn = false

        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInDelegate)

        viewModel.onStarClicked(TestData.userSession1)

        val starEvent: Event<SnackbarMessage>? =
            LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        // TODO change with actual resource used
        assertThat(
            starEvent?.getContentIfNotHandled()?.messageId,
            `is`(not(equalTo(R.string.reservation_request_succeeded)))
        )

        // Verify that the sign in dialog was triggered
        val signInEvent = LiveDataTestUtil.getValue(viewModel.navigateToSignInDialogAction)
        assertNotNull(signInEvent?.getContentIfNotHandled())
    }

    /** New reservation / waitlist **/

    @Test
    fun reservationReceived() = coroutineRule.runBlockingTest {
        // Create test use cases with test data
        val testUserId = "test"
        val source = TestUserEventDataSource()
        val loadSessionsUseCase = createTestLoadUserSessionsByDayUseCase(source)
        val signInDelegate = FakeSignInViewModelDelegate()
        val snackbarMessageManager = SnackbarMessageManager(FakePreferenceStorage())
        val viewModel = createScheduleViewModel(
            loadScheduleSessionsUseCase = loadSessionsUseCase,
            signInViewModelDelegate = signInDelegate,
            snackbarMessageManager = snackbarMessageManager
        )

        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser(testUserId)

        // Observe viewmodel to load sessions
        viewModel.scheduleUiData.observeForever { }

        // Observe snackbar so messages are received
        viewModel.snackBarMessage.observeForever { }

        // A session goes from not-reserved to reserved
        val oldValue = UserEventsResult(TestData.userEvents)
        val newValue = oldValue.copy(
            userEventsMessage = UserEventMessage(
                UserEventMessageChangeType.CHANGES_IN_RESERVATIONS
            )
        )
        source.newObservableUserEvents.value = newValue

        val reservationMessage: Event<SnackbarMessage>? =
            LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        assertThat(
            reservationMessage?.getContentIfNotHandled()?.messageId,
            `is`(equalTo(R.string.reservation_new))
        )
    }

    @Test
    fun waitlistReceived() = coroutineRule.runBlockingTest {
        // Create test use cases with test data
        val source = TestUserEventDataSource()
        val loadSessionsUseCase = createTestLoadUserSessionsByDayUseCase(source)
        val signInDelegate = FakeSignInViewModelDelegate()
        val snackbarMessageManager = SnackbarMessageManager(FakePreferenceStorage())
        val viewModel = createScheduleViewModel(
            loadScheduleSessionsUseCase = loadSessionsUseCase,
            signInViewModelDelegate = signInDelegate,
            snackbarMessageManager = snackbarMessageManager
        )

        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser("test")

        // Observe viewmodel to load sessions
        viewModel.scheduleUiData.observeForever {}

        // Observe snackbar so messages are received
        viewModel.snackBarMessage.observeForever { }

        // A session goes from not-reserved to reserved
        val oldValue = UserEventsResult(TestData.userEvents)
        val newValue = oldValue.copy(
            userEventsMessage = UserEventMessage(UserEventMessageChangeType.CHANGES_IN_WAITLIST)
        )

        source.newObservableUserEvents.value = newValue

        val waitlistMessage: Event<SnackbarMessage>? =
            LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        assertThat(
            waitlistMessage?.getContentIfNotHandled()?.messageId,
            `is`(equalTo(R.string.waitlist_new))
        )
    }

    @Test
    fun noLoggedInUser_showsReservationButton() = coroutineRule.runBlockingTest {
        // Given no logged in user
        val noFirebaseUser = null

        // Create ViewModel
        val observableFirebaseUserUseCase = FakeObserveUserAuthStateUseCase(
            user = Result.Success(noFirebaseUser),
            isRegistered = Result.Success(false),
            coroutineScope = coroutineRule.CoroutineScope(),
            coroutineDispatcher = testDispatcher
        )
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {},
            testDispatcher,
            testDispatcher,
            true
        )
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        viewModel.showReservations.observeForTesting {
            // Check that reservation buttons are shown
            assertEquals(true, LiveDataTestUtil.getValue(viewModel.showReservations))
        }
    }

    @Test
    fun loggedInUser_registered_showsReservationButton() = coroutineRule.runBlockingTest {
        // Given a logged in user
        val mockUser = mock<AuthenticatedUserInfoBasic> {
            on { getUid() }.doReturn("uuid")
            on { isSignedIn() }.doReturn(true)
        }

        // Who is registered
        val observableFirebaseUserUseCase = FakeObserveUserAuthStateUseCase(
            user = Result.Success(mockUser),
            isRegistered = Result.Success(true),
            coroutineScope = coroutineRule.CoroutineScope(),
            coroutineDispatcher = testDispatcher
        )
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {},
            testDispatcher,
            testDispatcher,
            true
        )
        // Create ViewModel
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        viewModel.showReservations.observeForTesting {
            // Check that reservation buttons are shown
            assertEquals(true, LiveDataTestUtil.getValue(viewModel.showReservations))
        }
    }

    @Test
    fun loggedInUser_notRegistered_hidesReservationButton() = coroutineRule.runBlockingTest {
        // Given a logged in user
        val mockUser = mock<AuthenticatedUserInfoBasic> {
            on { getUid() }.doReturn("uuid")
            on { isSignedIn() }.doReturn(true)
        }

        // Who isn't registered
        val observableFirebaseUserUseCase = FakeObserveUserAuthStateUseCase(
            user = Result.Success(mockUser),
            isRegistered = Result.Success(false),
            coroutineScope = coroutineRule.CoroutineScope(),
            coroutineDispatcher = testDispatcher
        )
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {},
            testDispatcher,
            testDispatcher,
            true
        )

        // Create ViewModel
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Observe signIn and registeredUser so messages are received
        signInViewModelComponent.observeRegisteredUser().observeForever { }
        signInViewModelComponent.observeSignedInUser().observeForever { }

        viewModel.showReservations.observeForTesting {
            // Check that *no* reservation buttons are shown
            assertEquals(false, LiveDataTestUtil.getValue(viewModel.showReservations))
        }
    }

    @Test
    fun scheduleHints_notShown_on_launch() = coroutineRule.runBlockingTest {
        val viewModel = createScheduleViewModel()
        viewModel.scheduleUiHintsShown.observeForTesting {
            val event = LiveDataTestUtil.getValue(viewModel.scheduleUiHintsShown)
            assertEquals(event?.getContentIfNotHandled(), false)
        }
    }

    @Test
    fun swipeRefresh_refreshesRemoteConfData() = coroutineRule.runBlockingTest {
        // Given a view model with a mocked remote data source
        val remoteDataSource = mock<ConferenceDataSource> {}
        val viewModel = createScheduleViewModel(
            refreshConferenceDataUseCase = RefreshConferenceDataUseCase(
                ConferenceDataRepository(
                    remoteDataSource = remoteDataSource,
                    boostrapDataSource = TestDataSource,
                    appDatabase = FakeAppDatabase()
                ),
                testDispatcher
            )
        )

        // When swipe refresh is called
        viewModel.onSwipeRefresh()

        // Then the remote data source attempts to fetch new data
        verify(remoteDataSource).getRemoteConferenceData()

        viewModel.swipeRefreshing.observeForTesting {
            // And the swipe refreshing status is set to false
            assertEquals(false, LiveDataTestUtil.getValue(viewModel.swipeRefreshing))
        }
    }

    @Test
    fun newDataFromConfRepo_scheduleUpdated() {
        val repo = ConferenceDataRepository(
            remoteDataSource = TestConfDataSourceSession0(),
            boostrapDataSource = BootstrapDataSourceSession3(),
            appDatabase = FakeAppDatabase()
        )

        val loadUserSessionsByDayUseCase = createTestLoadUserSessionsByDayUseCase(
            conferenceDataRepo = repo
        )
        val viewModel = createScheduleViewModel(
            loadScheduleSessionsUseCase = loadUserSessionsByDayUseCase,
            observeConferenceDataUseCase = ObserveConferenceDataUseCase(repo, testDispatcher)
        )

        // Observe viewmodel to load sessions
        viewModel.scheduleUiData.observeForever {}

        // Trigger a refresh on the repo
        repo.refreshCacheWithRemoteConferenceData()

        // The new value should be present
        val newValue = LiveDataTestUtil.getValue(viewModel.scheduleUiData)

        assertThat(
            newValue?.list?.first()?.session,
            `is`(IsEqual.equalTo(TestData.session0))
        )
    }

    private fun createScheduleViewModel(
        loadScheduleSessionsUseCase: LoadScheduleUserSessionsUseCase =
            createTestLoadUserSessionsByDayUseCase(),
        signInViewModelDelegate: SignInViewModelDelegate = FakeSignInViewModelDelegate(),
        starEventUseCase: StarEventAndNotifyUseCase = createStarEventUseCase(),
        snackbarMessageManager: SnackbarMessageManager = SnackbarMessageManager(
            FakePreferenceStorage()
        ),
        scheduleUiHintsShownUseCase: ScheduleUiHintsShownUseCase =
            FakeScheduleUiHintsShownUseCase(),
        getTimeZoneUseCase: GetTimeZoneUseCase = createGetTimeZoneUseCase(),
        topicSubscriber: TopicSubscriber = mock {},
        refreshConferenceDataUseCase: RefreshConferenceDataUseCase =
            RefreshConferenceDataUseCase(TestDataRepository, testDispatcher),
        observeConferenceDataUseCase: ObserveConferenceDataUseCase =
            ObserveConferenceDataUseCase(TestDataRepository, testDispatcher),
        analyticsHelper: AnalyticsHelper = FakeAnalyticsHelper()
    ): ScheduleViewModel {
        return ScheduleViewModel(
            loadScheduleUserSessionsUseCase = loadScheduleSessionsUseCase,
            signInViewModelDelegate = signInViewModelDelegate,
            starEventUseCase = starEventUseCase,
            scheduleUiHintsShownUseCase = scheduleUiHintsShownUseCase,
            topicSubscriber = topicSubscriber,
            snackbarMessageManager = snackbarMessageManager,
            getTimeZoneUseCase = getTimeZoneUseCase,
            refreshConferenceDataUseCase = refreshConferenceDataUseCase,
            observeConferenceDataUseCase = observeConferenceDataUseCase,
            analyticsHelper = analyticsHelper
        )
    }

    /**
     * Creates a test [LoadScheduleUserSessionsUseCase].
     */
    private fun createTestLoadUserSessionsByDayUseCase(
        userEventDataSource: UserEventDataSource = TestUserEventDataSource(),
        conferenceDataRepo: ConferenceDataRepository = TestDataRepository
    ): LoadScheduleUserSessionsUseCase {
        val sessionRepository = DefaultSessionRepository(conferenceDataRepo)
        val userEventRepository = DefaultSessionAndUserEventRepository(
            userEventDataSource, sessionRepository
        )

        return LoadScheduleUserSessionsUseCase(userEventRepository, testDispatcher)
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createExceptionUseCase(): LoadScheduleUserSessionsUseCase {
        return object : LoadScheduleUserSessionsUseCase(mock {}, testDispatcher) {
            override fun execute(parameters: LoadScheduleUserSessionsParameters):
                Flow<Result<LoadScheduleUserSessionsResult>> = flow {
                    throw Exception("Loading failed")
                }
        }
    }

    private fun createStarEventUseCase() = FakeStarEventUseCase(testDispatcher)

    private fun createGetTimeZoneUseCase() =
        GetTimeZoneUseCase(FakePreferenceStorage(), testDispatcher)
}

class TestRegisteredUserDataSource(private val isRegistered: Result<Boolean?>) :
    RegisteredUserDataSource {
    override fun observeUserChanges(userId: String): Flow<Result<Boolean?>> = flow {
        emit(isRegistered)
    }
}

class TestAuthStateUserDataSource(
    private val user: Result<AuthenticatedUserInfoBasic?>
) : AuthStateUserDataSource {
    override fun getBasicUserInfo(): Flow<Result<AuthenticatedUserInfoBasic?>> = flow {
        emit(user)
    }
}

class FakeObserveUserAuthStateUseCase(
    user: Result<AuthenticatedUserInfoBasic?>,
    isRegistered: Result<Boolean?>,
    coroutineScope: CoroutineScope,
    coroutineDispatcher: CoroutineDispatcher
) : ObserveUserAuthStateUseCase(
    TestRegisteredUserDataSource(isRegistered),
    TestAuthStateUserDataSource(user),
    mock {},
    coroutineScope,
    coroutineDispatcher
)

class FakeScheduleUiHintsShownUseCase : ScheduleUiHintsShownUseCase(
    preferenceStorage = FakePreferenceStorage(),
    dispatcher = TestCoroutineDispatcher()
)

class TestConfDataSourceSession0 : ConferenceDataSource {
    override fun getRemoteConferenceData(): ConferenceData? {
        return conferenceData
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return conferenceData
    }

    private val conferenceData = ConferenceData(
        sessions = listOf(TestData.session0),
        speakers = listOf(TestData.speaker1),
        rooms = emptyList(),
        codelabs = emptyList(),
        tags = listOf(TestData.androidTag, TestData.webTag),
        version = 42
    )
}

class BootstrapDataSourceSession3 : ConferenceDataSource {
    override fun getRemoteConferenceData(): ConferenceData? {
        throw NotImplementedError() // Not used
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return ConferenceData(
            sessions = listOf(TestData.session3),
            speakers = listOf(TestData.speaker1),
            rooms = emptyList(),
            codelabs = emptyList(),
            tags = listOf(TestData.androidTag, TestData.webTag),
            version = 42
        )
    }
}
