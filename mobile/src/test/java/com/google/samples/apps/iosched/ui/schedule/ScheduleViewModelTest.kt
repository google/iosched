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

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.TestData
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.session.agenda.AgendaRepository
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfoBasic
import com.google.samples.apps.iosched.shared.data.signin.datasources.AuthStateUserDataSource
import com.google.samples.apps.iosched.shared.data.signin.datasources.RegisteredUserDataSource
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessage
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.ScheduleUiHintsShownUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.RequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.domain.users.StarEventUseCase
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher.TagFilterMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeStarEventUseCase
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogParameters
import com.google.samples.apps.iosched.ui.schedule.day.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.schedule.filters.LoadTagFiltersUseCase
import com.google.samples.apps.iosched.ui.schedule.filters.TagFilter
import com.google.samples.apps.iosched.ui.signin.FirebaseSignInViewModelDelegate
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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

    @Test
    fun testDataIsLoaded_ObservablesUpdated() { // TODO: Very slow test (1s)
        // Create test use cases with test data
        val loadSessionsUseCase = LoadUserSessionsByDayUseCase(
            DefaultSessionAndUserEventRepository(
                TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
            )
        )
        val loadTagsUseCase = LoadTagFiltersUseCase(TagRepository(TestDataRepository))
        val signInDelegate = FakeSignInViewModelDelegate()

        // Create ViewModel with the use cases
        val viewModel = createScheduleViewModel(
            loadSessionsUseCase = loadSessionsUseCase,
            loadTagsUseCase = loadTagsUseCase,
            signInViewModelDelegate = signInDelegate
        )

        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser("test")

        // Observe viewmodel to load sessions
        viewModel.getSessionsForDay(DAY_1).observeForever() {}

        // Check that data were loaded correctly
        // Sessions
        for (day in ConferenceDay.values()) {

            assertEquals(
                TestData.userSessionMap[day],
                LiveDataTestUtil.getValue(viewModel.getSessionsForDay(day))
            )
        }
        assertFalse(LiveDataTestUtil.getValue(viewModel.isLoading)!!)
        // Tags
        assertEquals(TestData.tagFiltersList, LiveDataTestUtil.getValue(viewModel.tagFilters))
    }

    @Test
    fun profileClicked_whileLoggedIn_showsSignOutDialog() {
        // Given a ViewModel with a signed in user
        val signInViewModelDelegate = createSignInViewModelDelegate().apply {
            injectIsSignedIn = true
        }
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelDelegate)

        // When profile is clicked
        viewModel.onProfileClicked()

        // Then the sign out dialog should be shown
        val signOutEvent = LiveDataTestUtil.getValue(viewModel.navigateToSignOutDialogAction)
        assertNotNull(signOutEvent?.getContentIfNotHandled())
    }

    @Test
    fun profileClicked_whileLoggedOut_showsSignInDialog() {
        // Given a ViewModel with no signed in user
        val signInViewModelDelegate = createSignInViewModelDelegate().apply {
            injectIsSignedIn = false
        }
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelDelegate)

        // When profile is clicked
        viewModel.onProfileClicked()

        // Then the sign in dialog should ne shown
        val signInEvent = LiveDataTestUtil.getValue(viewModel.navigateToSignInDialogAction)
        assertNotNull(signInEvent?.getContentIfNotHandled())
    }

    @Test
    fun loggedInUser_setsProfileContentDescription() {
        // Given a mock firebase user
        val mockUser = mock<AuthenticatedUserInfoBasic> {
            on { getUid() }.doReturn("123")
            on { getPhotoUrl() }.doReturn(mock<Uri> {})
            on { isSignedIn() }.doReturn(true)
        }

        // Create ViewModel
        val observableFirebaseUserUseCase =
            FakeObserveUserAuthStateUseCase(
                user = Result.Success(mockUser),
                isRegistered = Result.Success(false)
            )
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {})
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Check that the expected content description is set
        assertEquals(
            R.string.sign_out,
            LiveDataTestUtil.getValue(viewModel.profileContentDesc)
        )
    }

    @Test
    fun noLoggedInUser_setsProfileContentDescription() {
        // Given no firebase user
        val noFirebaseUser = null

        // Create ViewModel
        val observableFirebaseUserUseCase =
            FakeObserveUserAuthStateUseCase(
                user = Result.Success(noFirebaseUser),
                isRegistered = Result.Success(false)
            )
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {})

        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Check that the expected content description is set
        assertEquals(R.string.sign_in, LiveDataTestUtil.getValue(viewModel.profileContentDesc))
    }

    @Test
    fun errorLoggingIn_setsProfileContentDescription() {
        // Given no firebase user
        val errorLoadingFirebaseUser = Result.Error(Exception())

        // Create ViewModel
        val observableFirebaseUserUseCase =
            FakeObserveUserAuthStateUseCase(
                user = errorLoadingFirebaseUser,
                isRegistered = Result.Success(false)
            )
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {})
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Check that the expected content description is set
        assertEquals(R.string.sign_in, LiveDataTestUtil.getValue(viewModel.profileContentDesc))
    }

    @Test
    fun testDataIsLoaded_Fails() {
        // Create ViewModel
        val viewModel = createScheduleViewModel()
        val errorMsg = LiveDataTestUtil.getValue(viewModel.errorMessage)
        assertTrue(errorMsg?.peekContent()?.isNotEmpty() ?: false)
    }

    /** Starring **/

    @Test
    fun testStarEvent() {
        // Create test use cases with test data
        val snackbarMessageManager = SnackbarMessageManager(FakePreferenceStorage())
        val viewModel = createScheduleViewModel(snackbarMessageManager = snackbarMessageManager)

        viewModel.onStarClicked(TestData.userEvents[0])

        val nextMessageEvent: Event<SnackbarMessage>? =
            LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        val message = nextMessageEvent?.getContentIfNotHandled()
        assertThat(message?.messageId, `is`(equalTo(R.string.event_starred)))
        assertThat(message?.actionId, `is`(equalTo(R.string.dont_show)))

        //TODO: check changes in data source
    }

    @Test
    fun testUnstarEvent() {
        // Create test use cases with test data
        val snackbarMessageManager = SnackbarMessageManager(FakePreferenceStorage())
        val viewModel = createScheduleViewModel(snackbarMessageManager = snackbarMessageManager)

        viewModel.onStarClicked(TestData.userEvents[1])

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

        viewModel.onStarClicked(TestData.userEvents[1])

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

    /** Reservations **/

    @Test
    fun testReserveEvent() {
        val reservationActionUseCaseMock = mock<ReservationActionUseCase> {
            on { observe() }.doReturn(MediatorLiveData())
        }
        val signInDelegate = FakeSignInViewModelDelegate()
        val viewModel = createScheduleViewModel(
            reservationActionUseCase = reservationActionUseCaseMock,
            signInViewModelDelegate = signInDelegate
        )
        val testUid = "testUid"
        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser(testUid)

        viewModel.onReservationClicked(TestData.session0, TestData.userEvents[4])

        verify(reservationActionUseCaseMock).execute(
            ReservationRequestParameters(
                testUid,
                TestData.session0.id, RequestAction()
            )
        )
    }

    @Test
    fun testReserveEvent_notLoggedIn() {
        // Create test use cases with test data
        val signInDelegate = FakeSignInViewModelDelegate()
        signInDelegate.injectIsSignedIn = false

        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInDelegate)

        viewModel.onReservationClicked(TestData.session0, TestData.userEvents[4])

        val event: Event<SnackbarMessage>? = LiveDataTestUtil.getValue(viewModel.snackBarMessage)
        // TODO change with actual resource used
        assertThat(
            event?.getContentIfNotHandled()?.messageId,
            `is`(not(equalTo(R.string.reservation_request_succeeded)))
        )

        // Then the sign in dialog should ne shown
        val signInEvent = LiveDataTestUtil.getValue(viewModel.navigateToSignInDialogAction)
        assertNotNull(signInEvent?.getContentIfNotHandled())
    }

    @Test
    fun testCancelEvent() {
        val signInDelegate = FakeSignInViewModelDelegate()
        val viewModel = createScheduleViewModel(
            signInViewModelDelegate = signInDelegate
        )
        val testUid = "testUid"
        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser(testUid)

        viewModel.onReservationClicked(TestData.session1, TestData.userEvents[0])

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

    /** New reservation / waitlist **/

    @Test
    fun reservationReceived() {
        // Create test use cases with test data
        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()
        val source = TestUserEventDataSource(userEventsResult)
        val loadSessionsUseCase = createTestLoadUserSessionsByDayUseCase(source)
        val signInDelegate = FakeSignInViewModelDelegate()
        val snackbarMessageManager = SnackbarMessageManager(FakePreferenceStorage())
        val viewModel = createScheduleViewModel(
            loadSessionsUseCase = loadSessionsUseCase,
            signInViewModelDelegate = signInDelegate,
            snackbarMessageManager = snackbarMessageManager
        )

        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser("test")

        // Observe viewmodel to load sessions
        viewModel.getSessionTimeDataForDay(ConferenceDay.DAY_1).observeForever() {}

        // Observe snackbar so messages are received
        viewModel.snackBarMessage.observeForever { }

        // A session goes from not-reserved to reserved
        val oldValue = LiveDataTestUtil.getValue(userEventsResult)
        val newValue = oldValue!!.copy(
            userEventsMessage = UserEventMessage(
                UserEventMessageChangeType.CHANGES_IN_RESERVATIONS
            )
        )

        userEventsResult.postValue(newValue)

        val reservationMessage: Event<SnackbarMessage>? =
            LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        assertThat(
            reservationMessage?.getContentIfNotHandled()?.messageId,
            `is`(equalTo(R.string.reservation_new))
        )
    }

    @Test
    fun waitlistReceived() {
        // Create test use cases with test data
        val userEventsResult: MutableLiveData<UserEventsResult> =
            MutableLiveData<UserEventsResult>()
        val source = TestUserEventDataSource(userEventsResult)
        val loadSessionsUseCase = createTestLoadUserSessionsByDayUseCase(source)
        val signInDelegate = FakeSignInViewModelDelegate()
        val snackbarMessageManager = SnackbarMessageManager(FakePreferenceStorage())
        val viewModel = createScheduleViewModel(
            loadSessionsUseCase = loadSessionsUseCase,
            signInViewModelDelegate = signInDelegate,
            snackbarMessageManager = snackbarMessageManager
        )

        // Kick off the viewmodel by loading a user.
        signInDelegate.loadUser("test")

        // Observe viewmodel to load sessions
        viewModel.getSessionTimeDataForDay(ConferenceDay.DAY_1).observeForever() {}

        // Observe snackbar so messages are received
        viewModel.snackBarMessage.observeForever { }

        // A session goes from not-reserved to reserved
        val oldValue = LiveDataTestUtil.getValue(userEventsResult)
        val newValue = oldValue!!.copy(
            userEventsMessage = UserEventMessage(UserEventMessageChangeType.CHANGES_IN_WAITLIST)
        )

        userEventsResult.postValue(newValue)

        val waitlistMessage: Event<SnackbarMessage>? =
            LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())

        assertThat(
            waitlistMessage?.getContentIfNotHandled()?.messageId,
            `is`(equalTo(R.string.waitlist_new))
        )
    }

    @Test
    fun noLoggedInUser_showsReservationButton() {
        // Given no logged in user
        val noFirebaseUser = null

        // Create ViewModel
        val observableFirebaseUserUseCase =
            FakeObserveUserAuthStateUseCase(
                user = Result.Success(noFirebaseUser),
                isRegistered = Result.Success(false)
            )
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {})

        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Check that reservation buttons are shown
        assertEquals(true, LiveDataTestUtil.getValue(viewModel.showReservations))
    }

    @Test
    fun loggedInUser_registered_showsReservationButton() {
        // Given a logged in user
        val mockUser = mock<AuthenticatedUserInfoBasic> {
            on { isSignedIn() }.doReturn(true)
        }

        // Who is registered
        val observableFirebaseUserUseCase =
            FakeObserveUserAuthStateUseCase(
                user = Result.Success(mockUser),
                isRegistered = Result.Success(true)
            )
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {})

        // Create ViewModel
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Check that reservation buttons are shown
        assertEquals(true, LiveDataTestUtil.getValue(viewModel.showReservations))
    }

    @Test
    fun loggedInUser_notRegistered_hidesReservationButton() {
        // Given a logged in user
        val mockUser = mock<AuthenticatedUserInfoBasic> {
            on { isSignedIn() }.doReturn(true)
        }

        // Who isn't registered
        val observableFirebaseUserUseCase =
            FakeObserveUserAuthStateUseCase(
                user = Result.Success(mockUser),
                isRegistered = Result.Success(false)
            )
        val signInViewModelComponent = FirebaseSignInViewModelDelegate(
            observableFirebaseUserUseCase,
            mock {})

        // Create ViewModel
        val viewModel = createScheduleViewModel(signInViewModelDelegate = signInViewModelComponent)

        // Check that *no* reservation buttons are shown
        assertEquals(false, LiveDataTestUtil.getValue(viewModel.showReservations))
    }

    @Test
    fun scheduleHints_notShown_on_launch() {
        val viewModel = createScheduleViewModel()

        val event = LiveDataTestUtil.getValue(viewModel.scheduleUiHintsShown)
        assertEquals(event?.getContentIfNotHandled(), false)
    }

    private fun createScheduleViewModel(
        loadSessionsUseCase: LoadUserSessionsByDayUseCase =
            createTestLoadUserSessionsByDayUseCase(),
        loadAgendaUseCase: LoadAgendaUseCase = createAgendaExceptionUseCase(),
        loadTagsUseCase: LoadTagFiltersUseCase = createTagsExceptionUseCase(),
        signInViewModelDelegate: SignInViewModelDelegate = createSignInViewModelDelegate(),
        starEventUseCase: StarEventUseCase = createStarEventUseCase(),
        reservationActionUseCase: ReservationActionUseCase = createReservationActionUseCase(),
        snackbarMessageManager: SnackbarMessageManager = SnackbarMessageManager(
            FakePreferenceStorage()
        ),
        scheduleUiHintsShownUseCase: ScheduleUiHintsShownUseCase =
            FakeScheduleUiHintsShownUseCase(),
        getTimeZoneUseCase: GetTimeZoneUseCase = createGetTimeZoneUseCase(),
        topicSubscriber: TopicSubscriber = mock {}
    ): ScheduleViewModel {
        return ScheduleViewModel(
            loadUserSessionsByDayUseCase = loadSessionsUseCase,
            loadAgendaUseCase = loadAgendaUseCase,
            loadTagFiltersUseCase = loadTagsUseCase,
            signInViewModelDelegate = signInViewModelDelegate,
            starEventUseCase = starEventUseCase,
            reservationActionUseCase = reservationActionUseCase,
            snackbarMessageManager = snackbarMessageManager,
            scheduleUiHintsShownUseCase = scheduleUiHintsShownUseCase,
            getTimeZoneUseCase = getTimeZoneUseCase,
            topicSubscriber = topicSubscriber
        )
    }

    /**
     * Creates a test [LoadUserSessionsByDayUseCase].
     */
    private fun createTestLoadUserSessionsByDayUseCase(
        userEventDataSource: UserEventDataSource = TestUserEventDataSource()
    ): LoadUserSessionsByDayUseCase {

        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val userEventRepository = DefaultSessionAndUserEventRepository(
            userEventDataSource, sessionRepository
        )

        return LoadUserSessionsByDayUseCase(userEventRepository)
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createTagsExceptionUseCase(): LoadTagFiltersUseCase {
        return object : LoadTagFiltersUseCase(TagRepository(TestDataRepository)) {
            override fun execute(parameters: TagFilterMatcher): List<TagFilter> {
                throw Exception("Testing exception")
            }
        }
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createAgendaExceptionUseCase(): LoadAgendaUseCase {
        return object : LoadAgendaUseCase(AgendaRepository()) {
            override fun execute(parameters: Unit): List<Block> {
                throw Exception("Testing exception")
            }
        }
    }

    private fun createSignInViewModelDelegate() = FakeSignInViewModelDelegate()

    private fun createStarEventUseCase() = FakeStarEventUseCase()

    private fun createReservationActionUseCase() = object : ReservationActionUseCase(
        DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
        )
    ) {}

    private fun createGetTimeZoneUseCase() =
        object : GetTimeZoneUseCase(FakePreferenceStorage()) {}
}

class TestRegisteredUserDataSource(private val isRegistered: Result<Boolean?>) :
    RegisteredUserDataSource {
    override fun listenToUserChanges(userId: String) {}

    override fun observeResult(): LiveData<Result<Boolean?>?> {
        return MutableLiveData<Result<Boolean?>?>().apply { value = isRegistered }
    }

    override fun setAnonymousValue() {}
}

class TestAuthStateUserDataSource(
    private val user: Result<AuthenticatedUserInfoBasic?>?
) : AuthStateUserDataSource {
    override fun startListening() {}

    override fun getBasicUserInfo(): LiveData<Result<AuthenticatedUserInfoBasic?>> =
        MutableLiveData<Result<AuthenticatedUserInfoBasic?>>().apply { value = user }

    override fun clearListener() {}
}

class FakeObserveUserAuthStateUseCase(
    user: Result<AuthenticatedUserInfoBasic?>?,
    isRegistered: Result<Boolean?>
) : ObserveUserAuthStateUseCase(
    TestRegisteredUserDataSource(isRegistered),
    TestAuthStateUserDataSource(user),
    mock {}
)

class FakeScheduleUiHintsShownUseCase : ScheduleUiHintsShownUseCase(
    preferenceStorage = FakePreferenceStorage()
)
