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

package com.google.samples.apps.iosched.shared.domain.users

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCaseResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.CANCEL
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.REQUEST
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.TestData
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.SyncExecutorRule
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [ReservationActionUseCase].
 */
class ReservationActionUseCaseTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncExecutorRule = SyncExecutorRule()


    @Test
    fun sessionIsRequestedSuccessfully() {
        val useCase = ReservationActionUseCase(TestUserEventRepository)

        val resultLiveData = useCase.observe()

        useCase.execute(ReservationRequestParameters(
                "userTest",
                TestData.session0.id,
                REQUEST))

        val result = LiveDataTestUtil.getValue(resultLiveData)
        Assert.assertEquals(result, Result.Success(ReservationRequestAction.REQUEST))
    }

    @Test
    fun sessionIsCanceledSuccessfully() {
        val useCase = ReservationActionUseCase(TestUserEventRepository)

        val resultLiveData = useCase.observe()

        useCase.execute(ReservationRequestParameters(
                "userTest", TestData.session0.id,
                CANCEL))

        val result = LiveDataTestUtil.getValue(resultLiveData)
        Assert.assertEquals(result, Result.Success(ReservationRequestAction.CANCEL))
    }


    @Test
    fun requestFails() {
        val useCase = ReservationActionUseCase(FailingUserEventRepository)

        val resultLiveData = useCase.observe()

        useCase.execute(ReservationRequestParameters(
                "userTest", TestData.session0.id,
                CANCEL))

        val result = LiveDataTestUtil.getValue(resultLiveData)
        assertTrue(result is Result.Error)
    }
}

object TestUserEventRepository : SessionAndUserEventRepository {
    override fun getObservableUserEvents(userId: String?
    ): LiveData<Result<LoadUserSessionsByDayUseCaseResult>> {
        TODO("not implemented")
    }

    override fun getObservableUserEvent(
            userId: String?,
            eventId: String
    ): LiveData<Result<LoadUserSessionUseCaseResult>> {
        TODO("not implemented")
    }

    override fun starEvent(userId: String, userEvent: UserEvent):
            LiveData<Result<StarUpdatedStatus>> {
        TODO("not implemented")
    }

    override fun changeReservation(
            userId: String, sessionId: String, action: ReservationRequestAction
    ): LiveData<Result<ReservationRequestAction>> {

        val result = MutableLiveData<Result<ReservationRequestAction>>()
        result.postValue(Result.Success(
                if (action == ReservationRequestAction.REQUEST) ReservationRequestAction.REQUEST
                else ReservationRequestAction.CANCEL)
        )
        return result
    }

}

object FailingUserEventRepository : SessionAndUserEventRepository {
    override fun getObservableUserEvents(userId: String?
    ): LiveData<Result<LoadUserSessionsByDayUseCaseResult>> {
        TODO("not implemented")
    }

    override fun getObservableUserEvent(
            userId: String?,
            eventId: String
    ): LiveData<Result<LoadUserSessionUseCaseResult>> {
        TODO("not implemented")
    }

    override fun starEvent(userId: String, userEvent: UserEvent):
            LiveData<Result<StarUpdatedStatus>> {
        TODO("not implemented")
    }

    override fun changeReservation(userId: String,
                                   sessionId: String,
                                   action: ReservationRequestAction):
            LiveData<Result<ReservationRequestAction>> {
        throw Exception("Test")
    }

}
