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
import com.google.samples.apps.iosched.model.ConferenceData
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.model.TestDataSource
import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.ConferenceDataSource
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
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
import com.google.samples.apps.iosched.shared.domain.prefs.StopSnackbarActionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadScheduleUserSessionsParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadScheduleUserSessionsResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadScheduleUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.ObserveConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.CoroutineScope
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.fakes.FakeAppDatabase
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.signin.FirebaseSignInViewModelDelegate
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    private val coroutineScope = coroutineRule.CoroutineScope()

    @Test
    fun testDataIsLoaded_ObservablesUpdated() = runTest {
        // Create a delegate so we can load a user
        val signInDelegate = FakeSignInViewModelDelegate()

        // Create ViewModel with the use cases
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInDelegate)

        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser("test")

        // Observe viewmodel to load sessions
        val scheduleUiData = viewModel.scheduleUiData.first()

        // Check that data were loaded correctly
        assertEquals(
            TestData.userSessionList,
            scheduleUiData.list
        )
        assertFalse(viewModel.isLoading.first())
    }

    @Test
    fun testDataIsLoaded_Fails() = runTest {
        // Create ViewModel
        val viewModel = createScheduleViewModel(
            loadScheduleSessionsUseCase = createExceptionUseCase()
        )
        // Trigger data load
        viewModel.scheduleUiData.first()

        assertNotNull(viewModel.errorMessage.first())
    }

    /** New reservation / waitlist **/

    @Test
    fun reservationReceived() = runTest {
        // Create test use cases with test data
        val testUserId = "test"
        val source = TestUserEventDataSource()
        val loadSessionsUseCase = createTestLoadUserSessionsByDayUseCase(source)
        val signInDelegate = FakeSignInViewModelDelegate()
        val snackbarMessageManager = createSnackbarMessageManager()
        val viewModel = createScheduleViewModel(
            loadScheduleSessionsUseCase = loadSessionsUseCase,
            signInViewModelDelegate = signInDelegate,
            snackbarMessageManager = snackbarMessageManager
        )

        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser(testUserId)

        // Observe viewmodel to load sessions
        viewModel.scheduleUiData.first()

        // A session goes from not-reserved to reserved
        val oldValue = UserEventsResult(TestData.userEvents)
        val newValue = oldValue.copy(
            userEventsMessage = UserEventMessage(
                UserEventMessageChangeType.CHANGES_IN_RESERVATIONS
            )
        )
        source.newObservableUserEvents.value = newValue

        val message = snackbarMessageManager.currentSnackbar.value
        assertEquals(
            message?.messageId,
            R.string.reservation_new
        )
    }

    @Test
    fun waitlistReceived() = runTest {
        // Create test use cases with test data
        val source = TestUserEventDataSource()
        val loadSessionsUseCase = createTestLoadUserSessionsByDayUseCase(source)
        val signInDelegate = FakeSignInViewModelDelegate()
        val snackbarMessageManager = createSnackbarMessageManager()
        val viewModel = createScheduleViewModel(
            loadScheduleSessionsUseCase = loadSessionsUseCase,
            signInViewModelDelegate = signInDelegate,
            snackbarMessageManager = snackbarMessageManager
        )

        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser("test")

        // Observe viewmodel to load sessions
        viewModel.scheduleUiData.first()

        // A session goes from not-reserved to reserved
        val oldValue = UserEventsResult(TestData.userEvents)
        val newValue = oldValue.copy(
            userEventsMessage = UserEventMessage(UserEventMessageChangeType.CHANGES_IN_WAITLIST)
        )

        source.newObservableUserEvents.value = newValue

        val message = snackbarMessageManager.currentSnackbar.value
        assertEquals(
            message?.messageId,
            R.string.waitlist_new
        )
    }

    @Test
    fun noLoggedInUser_showsReservationButton() = runTest {
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
            true,
            coroutineScope
        )
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Check that reservation buttons are shown
        assertTrue(viewModel.showReservations.first())
    }

    @Test
    fun loggedInUser_registered_showsReservationButton() = runTest {
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
            true,
            coroutineScope
        )
        // Create ViewModel
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Trigger data load
        viewModel.userInfo.first()
        viewModel.isUserSignedIn.first()
        viewModel.isUserRegistered.first()

        // Check that reservation buttons are shown
        assertTrue(viewModel.showReservations.first())
    }

    @Test
    fun loggedInUser_notRegistered_hidesReservationButton() = runTest {
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
            true,
            coroutineRule.CoroutineScope()
        )

        // Create ViewModel
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Trigger data load
        viewModel.userInfo.first()
        viewModel.isUserSignedIn.first()
        viewModel.isUserRegistered.first()

        // Check that *no* reservation buttons are shown
        assertFalse(viewModel.showReservations.first())
    }

    @Test
    fun scheduleHints_shownOnLaunch() = runTest {
        val viewModel = createScheduleViewModel()
        val firstNavAction = viewModel.navigationActions.firstOrNull()
        assertEquals(firstNavAction, ScheduleNavigationAction.ShowScheduleUiHints)
    }

    @Test
    fun swipeRefresh_refreshesRemoteConfData() = runTest {
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

        // And the swipe refreshing status is set to false
        assertEquals(false, viewModel.swipeRefreshing.first())
    }

    @Test
    fun newDataFromConfRepo_scheduleUpdated() = runTest {
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
        viewModel.scheduleUiData.first()

        // Trigger a refresh on the repo
        repo.refreshCacheWithRemoteConferenceData()

        // The new value should be present
        val newValue = viewModel.scheduleUiData.first()

        assertThat(
            newValue.list?.first()?.session,
            `is`(IsEqual.equalTo(TestData.session0))
        )
    }

    @Test
    fun scrollToEvent_beforeconference() {
        scrollToEvent_beforeConference(dayIndex = 0, targetPosition = 0)
    }

    @Test
    fun scrollToEvent_beforeConference_clickOnSecondDay() {
        scrollToEvent_beforeConference(dayIndex = 1, targetPosition = 2)
    }

    private fun scrollToEvent_beforeConference(dayIndex: Int, targetPosition: Int) =
        runTest {
            val viewModel = createScheduleViewModel()

            // Start observing
            viewModel.scheduleUiData.first()

            // Trigger to generate indexer
            viewModel.scrollToStartOfDay(TestData.TestConferenceDays[0])

            val result = mutableListOf<ScheduleScrollEvent>()
            val job = launch(UnconfinedTestDispatcher()) {
                viewModel.scrollToEvent.toList(result)
            }

            // Trigger to generate a result in scrollToEvent
            viewModel.scrollToStartOfDay(TestData.TestConferenceDays[dayIndex])

            assertTrue(result.size == 1)
            assertEquals(
                result[0],
                ScheduleScrollEvent(targetPosition = targetPosition, smoothScroll = false)
            )

            job.cancel()
        }

    @Test
    fun scrollToEvent_beforeConference_userHasInteracted() = runTest {
        val viewModel = createScheduleViewModel()

        viewModel.userHasInteracted = true

        // Start observing
        viewModel.scheduleUiData.first()

        // Trigger to generate indexer
        viewModel.scrollToStartOfDay(TestData.TestConferenceDays[0])

        val result = mutableListOf<ScheduleScrollEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.scrollToEvent.toList(result)
        }

        // Trigger to generate a result in scrollToEvent
        viewModel.scrollToStartOfDay(TestData.TestConferenceDays[1])
        assertEquals(1, result.size)
        assertEquals(result[0], ScheduleScrollEvent(targetPosition = 2, smoothScroll = false))

        job.cancel()
    }

    private fun createScheduleViewModel(
        loadScheduleSessionsUseCase: LoadScheduleUserSessionsUseCase =
            createTestLoadUserSessionsByDayUseCase(),
        signInViewModelDelegate: SignInViewModelDelegate = FakeSignInViewModelDelegate(),
        snackbarMessageManager: SnackbarMessageManager = createSnackbarMessageManager(),
        scheduleUiHintsShownUseCase: ScheduleUiHintsShownUseCase =
            FakeScheduleUiHintsShownUseCase(testDispatcher),
        getTimeZoneUseCase: GetTimeZoneUseCase = createGetTimeZoneUseCase(),
        topicSubscriber: TopicSubscriber = mock {},
        refreshConferenceDataUseCase: RefreshConferenceDataUseCase =
            RefreshConferenceDataUseCase(TestDataRepository, testDispatcher),
        observeConferenceDataUseCase: ObserveConferenceDataUseCase =
            ObserveConferenceDataUseCase(TestDataRepository, testDispatcher)
    ): ScheduleViewModel {
        return ScheduleViewModel(
            loadScheduleUserSessionsUseCase = loadScheduleSessionsUseCase,
            signInViewModelDelegate = signInViewModelDelegate,
            scheduleUiHintsShownUseCase = scheduleUiHintsShownUseCase,
            topicSubscriber = topicSubscriber,
            snackbarMessageManager = snackbarMessageManager,
            getTimeZoneUseCase = getTimeZoneUseCase,
            refreshConferenceDataUseCase = refreshConferenceDataUseCase,
            observeConferenceDataUseCase = observeConferenceDataUseCase
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

    private fun createGetTimeZoneUseCase() =
        GetTimeZoneUseCase(FakePreferenceStorage(), testDispatcher)

    private fun createSnackbarMessageManager(
        preferenceStorage: PreferenceStorage = FakePreferenceStorage()
    ): SnackbarMessageManager {
        return SnackbarMessageManager(
            preferenceStorage,
            coroutineRule.CoroutineScope(),
            StopSnackbarActionUseCase(preferenceStorage, testDispatcher)
        )
    }
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

class FakeScheduleUiHintsShownUseCase(
    dispatcher: CoroutineDispatcher
) : ScheduleUiHintsShownUseCase(
    preferenceStorage = FakePreferenceStorage(),
    dispatcher = dispatcher,
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
