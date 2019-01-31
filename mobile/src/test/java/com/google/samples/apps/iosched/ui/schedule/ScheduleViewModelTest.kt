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

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.ConferenceData
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.MobileTestData
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.model.TestDataSource
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.ConferenceDataSource
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.data.signin.datasources.AuthStateUserDataSource
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.domain.RefreshConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.LoadSelectedFiltersUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.SaveSelectedFiltersUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.GetConferenceDaysUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.ObserveConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakeAppDatabase
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeStarEventUseCase
import com.google.samples.apps.iosched.test.util.fakes.FakeThemedActivityDelegate
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.ThemedActivityDelegate
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.day.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.schedule.filters.EventFilter
import com.google.samples.apps.iosched.ui.schedule.filters.LoadEventFiltersUseCase
import com.google.samples.apps.iosched.ui.signin.FirebaseSignInViewModelDelegate
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
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

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var viewModelDelegate: FakeSignInViewModelDelegate

    @Before
    fun setUp() {
        viewModelDelegate = FakeSignInViewModelDelegate()
    }

    @After
    fun tearDown() {
        viewModelDelegate.closeChannel()
    }

    @Test
    fun testDataIsLoaded_ObservablesUpdated() = coroutineRule.runBlockingTest {
        // Create test use cases with test data
        val loadSessionsUseCase = LoadUserSessionsByDayUseCase(
            DefaultSessionAndUserEventRepository(
                TestUserEventDataSource(),
                DefaultSessionRepository(TestDataRepository)
            ),
            coroutineRule.testDispatcher
        )

        val loadTagsUseCase =
            LoadEventFiltersUseCase(TagRepository(TestDataRepository), coroutineRule.testDispatcher)

        // Create ViewModel with the use cases
        val viewModel = createScheduleViewModel(
            loadSessionsUseCase = loadSessionsUseCase,
            loadTagsUseCase = loadTagsUseCase,
            signInViewModelDelegate = viewModelDelegate
        )

        // Kick off the viewmodel by loading a user.
        viewModelDelegate.loadUser("test")

        // Observe viewmodel to load sessions
        viewModel.getSessionTimeDataForDay(0).observeForever {}

        // Check that data were loaded correctly
        // Sessions
        TestData.TestConferenceDays.forEachIndexed { index, day ->
            assertEquals(
                TestData.userSessionMap[day],
                LiveDataTestUtil.getValue(viewModel.getSessionTimeDataForDay(index))?.list
            )
        }
        assertFalse(LiveDataTestUtil.getValue(viewModel.isLoading)!!)
        // Tags
        val loadedFilters = LiveDataTestUtil.getValue(viewModel.eventFilters)
        assertTrue(loadedFilters?.containsAll(MobileTestData.tagFiltersList) ?: false)
    }

    @Test
    fun profileClicked_whileLoggedIn_showsSignOutDialog() = coroutineRule.runBlockingTest {

        // Given a ViewModel with a signed in user
        viewModelDelegate.injectIsSignedIn = true

        val viewModel = createScheduleViewModel(signInViewModelDelegate = viewModelDelegate)

        // When profile is clicked
        viewModel.onProfileClicked()

        // Then the sign out dialog should be shown
        val signOutEvent = LiveDataTestUtil.getValue(viewModel.navigateToSignOutDialogAction)
        assertNotNull(signOutEvent?.getContentIfNotHandled())
    }

    @Test
    fun profileClicked_whileLoggedOut_showsSignInDialog() = coroutineRule.runBlockingTest {
        // Given a ViewModel with no signed in user
        viewModelDelegate.injectIsSignedIn = false

        val viewModel = createScheduleViewModel(signInViewModelDelegate = viewModelDelegate)

        // When profile is clicked
        viewModel.onProfileClicked()

        // Then the sign in dialog should ne shown
        val signInEvent = LiveDataTestUtil.getValue(viewModel.navigateToSignInDialogAction)
        assertNotNull(signInEvent?.getContentIfNotHandled())
    }

    @Test
    fun loggedInUser_setsProfileContentDescription() = coroutineRule.runBlockingTest {
        // Given a mock firebase user
        val mockUser = mock<AuthenticatedUserInfo> {
            on { getUid() }.doReturn("123")
            on { getPhotoUrl() }.doReturn(mock<Uri> {})
            on { isSignedIn() }.doReturn(true)
        }

        // Create ViewModel
        val observableFirebaseUserUseCase =
            FakeObserveUserAuthStateUseCase(Result.Success(mockUser), coroutineRule.testDispatcher)
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {},
            coroutineRule.testDispatcher,
            coroutineRule.testDispatcher
        )
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Check that the expected content description is set
        assertEquals(
            R.string.sign_out,
            LiveDataTestUtil.getValue(viewModel.profileContentDesc)
        )
    }

    @Test
    fun noLoggedInUser_setsProfileContentDescription() = coroutineRule.runBlockingTest {
        // Given no firebase user
        val noFirebaseUser = null

        // Create ViewModel
        val observableFirebaseUserUseCase = FakeObserveUserAuthStateUseCase(
            Result.Success(noFirebaseUser), coroutineRule.testDispatcher
        )
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {},
            coroutineRule.testDispatcher,
            coroutineRule.testDispatcher
        )

        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Check that the expected content description is set
        assertEquals(R.string.sign_in, LiveDataTestUtil.getValue(viewModel.profileContentDesc))
    }

    @Test
    fun errorLoggingIn_setsProfileContentDescription() = coroutineRule.runBlockingTest {
        // Given no firebase user
        val errorLoadingFirebaseUser = Result.Error(Exception())

        // Create ViewModel
        val observableFirebaseUserUseCase =
            FakeObserveUserAuthStateUseCase(errorLoadingFirebaseUser, coroutineRule.testDispatcher)
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {},
            coroutineRule.testDispatcher,
            coroutineRule.testDispatcher
        )
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Check that the expected content description is set
        assertEquals(R.string.sign_in, LiveDataTestUtil.getValue(viewModel.profileContentDesc))
    }

    @Test
    fun testDataIsLoaded_Fails() = coroutineRule.runBlockingTest {
        // Create ViewModel
        val viewModel = createScheduleViewModel()
        val errorMsg = LiveDataTestUtil.getValue(viewModel.errorMessage)
        assertTrue(errorMsg?.peekContent()?.isNotEmpty() ?: false)
    }

    /** Starring **/

    @Test
    fun testStarEvent() = coroutineRule.runBlockingTest {
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
    fun testUnstarEvent() = coroutineRule.runBlockingTest {
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
    fun testStar_notLoggedInUser() = coroutineRule.runBlockingTest {
        // Create test use cases with test data
        viewModelDelegate.injectIsSignedIn = false

        val viewModel = createScheduleViewModel(signInViewModelDelegate = viewModelDelegate)

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
                coroutineRule.testDispatcher
            )
        )

        // When swipe refresh is called
        viewModel.onSwipeRefresh()

        // Then the remote data source attempts to fetch new data
        verify(remoteDataSource).getRemoteConferenceData()

        // And the swipe refreshing status is set to false
        assertEquals(false, LiveDataTestUtil.getValue(viewModel.swipeRefreshing))
    }

    @Test
    fun newDataFromConfRepo_scheduleUpdated() = coroutineRule.runBlockingTest {
        val repo = createTestConferenceDataRepository()

        val loadUserSessionsByDayUseCase = createTestLoadUserSessionsByDayUseCase(
            conferenceDataRepo = repo
        )
        val viewModel = createScheduleViewModel(
            loadSessionsUseCase = loadUserSessionsByDayUseCase,
            observeConferenceDataUseCase = ObserveConferenceDataUseCase(
                repo, coroutineRule.testDispatcher
            )
        )

        // Trigger a refresh on the repo
        repo.refreshCacheWithRemoteConferenceData()

        // The new value should be present
        val newValue = LiveDataTestUtil.getValue(viewModel.getSessionTimeDataForDay(0))

        assertThat(newValue?.list?.first()?.session, `is`(IsEqual.equalTo(TestData.session0)))
    }

    private fun createTestConferenceDataRepository(): ConferenceDataRepository {
        return object : ConferenceDataRepository(
            remoteDataSource = TestConfDataSourceSession0(),
            boostrapDataSource = BootstrapDataSourceSession3(),
            appDatabase = FakeAppDatabase()
        ) {
            override fun getConferenceDays(): List<ConferenceDay> = TestData.TestConferenceDays
        }
    }

    private fun createScheduleViewModel(
        loadSessionsUseCase: LoadUserSessionsByDayUseCase =
            createTestLoadUserSessionsByDayUseCase(),
        getConferenceDaysUseCase: GetConferenceDaysUseCase = GetConferenceDaysUseCase(
            TestDataRepository
        ),
        loadTagsUseCase: LoadEventFiltersUseCase = createEventFiltersExceptionUseCase(),
        signInViewModelDelegate: SignInViewModelDelegate = viewModelDelegate,
        starEventUseCase: StarEventAndNotifyUseCase = createStarEventUseCase(),
        snackbarMessageManager: SnackbarMessageManager = SnackbarMessageManager(
            FakePreferenceStorage()
        ),
        getTimeZoneUseCase: GetTimeZoneUseCase = createGetTimeZoneUseCase(),
        topicSubscriber: TopicSubscriber = mock {},
        refreshConferenceDataUseCase: RefreshConferenceDataUseCase =
            RefreshConferenceDataUseCase(TestDataRepository, coroutineRule.testDispatcher),
        observeConferenceDataUseCase: ObserveConferenceDataUseCase =
            ObserveConferenceDataUseCase(TestDataRepository, coroutineRule.testDispatcher),
        loadSelectedFiltersUseCase: LoadSelectedFiltersUseCase =
            LoadSelectedFiltersUseCase(FakePreferenceStorage(), coroutineRule.testDispatcher),
        saveSelectedFiltersUseCase: SaveSelectedFiltersUseCase =
            SaveSelectedFiltersUseCase(FakePreferenceStorage(), coroutineRule.testDispatcher),
        analyticsHelper: AnalyticsHelper = FakeAnalyticsHelper(),
        themedActivityDelegate: ThemedActivityDelegate = FakeThemedActivityDelegate()
    ): ScheduleViewModel {

        val testUseEventDataSource = TestUserEventDataSource()

        return ScheduleViewModel(
            loadUserSessionsByDayUseCase = loadSessionsUseCase,
            getConferenceDaysUseCase = getConferenceDaysUseCase,
            loadEventFiltersUseCase = loadTagsUseCase,
            signInViewModelDelegate = signInViewModelDelegate,
            starEventUseCase = starEventUseCase,
            topicSubscriber = topicSubscriber,
            snackbarMessageManager = snackbarMessageManager,
            getTimeZoneUseCase = getTimeZoneUseCase,
            refreshConferenceDataUseCase = refreshConferenceDataUseCase,
            observeConferenceDataUseCase = observeConferenceDataUseCase,
            loadSelectedFiltersUseCase = loadSelectedFiltersUseCase,
            saveSelectedFiltersUseCase = saveSelectedFiltersUseCase,
            analyticsHelper = analyticsHelper,
            themedActivityDelegate = themedActivityDelegate
        )
    }

    /**
     * Creates a test [LoadUserSessionsByDayUseCase].
     */
    private fun createTestLoadUserSessionsByDayUseCase(
        userEventDataSource: UserEventDataSource = TestUserEventDataSource(),
        conferenceDataRepo: ConferenceDataRepository = TestDataRepository
    ): LoadUserSessionsByDayUseCase {
        val sessionRepository = DefaultSessionRepository(conferenceDataRepo)
        val userEventRepository = DefaultSessionAndUserEventRepository(
            userEventDataSource, sessionRepository
        )

        return LoadUserSessionsByDayUseCase(userEventRepository, coroutineRule.testDispatcher)
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createEventFiltersExceptionUseCase(): LoadEventFiltersUseCase {
        return object : LoadEventFiltersUseCase(
            TagRepository(TestDataRepository), coroutineRule.testDispatcher
        ) {
            override fun execute(parameters: UserSessionMatcher): List<EventFilter> {
                throw Exception("Testing exception")
            }
        }
    }

    private fun createStarEventUseCase() = FakeStarEventUseCase(coroutineRule.testDispatcher)

    private fun createGetTimeZoneUseCase() =
        object : GetTimeZoneUseCase(FakePreferenceStorage(), coroutineRule.testDispatcher) {}
}

class TestAuthStateUserDataSource(
    private val result: Result<AuthenticatedUserInfo?>
) : AuthStateUserDataSource {

    override fun getBasicUserInfo(): Flow<Result<AuthenticatedUserInfo?>> =
        flow { emit(result) }
}

class FakeObserveUserAuthStateUseCase(
    user: Result<AuthenticatedUserInfo?>,
    dispatcher: CoroutineDispatcher
) : ObserveUserAuthStateUseCase(TestAuthStateUserDataSource(user), dispatcher)

class TestConfDataSourceSession0 : ConferenceDataSource {
    override fun getRemoteConferenceData(): ConferenceData? {
        return conferenceData
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return conferenceData
    }

    private val conferenceData = ConferenceData(
        sessions = listOf(TestData.session0),
        tags = listOf(TestData.androidTag, TestData.webTag),
        speakers = listOf(TestData.speaker1),
        rooms = emptyList(),
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
            tags = listOf(TestData.androidTag, TestData.webTag),
            speakers = listOf(TestData.speaker1),
            rooms = emptyList(),
            version = 42
        )
    }
}
