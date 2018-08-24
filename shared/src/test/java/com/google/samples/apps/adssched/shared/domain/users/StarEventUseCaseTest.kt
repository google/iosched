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

package com.google.samples.apps.adssched.shared.domain.users

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.adssched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.adssched.model.SessionId
import com.google.samples.apps.adssched.model.userdata.UserEvent
import com.google.samples.apps.adssched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.adssched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.adssched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.adssched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.adssched.shared.domain.sessions.LoadUserSessionUseCaseResult
import com.google.samples.apps.adssched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.adssched.shared.model.TestDataRepository
import com.google.samples.apps.adssched.shared.result.Result
import com.google.samples.apps.adssched.shared.util.SyncExecutorRule
import com.google.samples.apps.adssched.test.data.TestData
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [StarEventUseCase]
 */
class StarEventUseCaseTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncExecutorRule = SyncExecutorRule()

    @Test
    fun sessionIsStarredSuccessfully() {
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
        )
        val useCase = StarEventUseCase(testUserEventRepository)

        val resultLiveData = useCase.observe()

        useCase.execute(StarEventParameter("userIdTest", TestData.userEvents[1]))

        val result = LiveDataTestUtil.getValue(resultLiveData)
        Assert.assertEquals(result, Result.Success(StarUpdatedStatus.STARRED))
    }

    @Test
    fun sessionIsStarredUnsuccessfully() {
        val useCase = StarEventUseCase(FailingSessionAndUserEventRepository)

        val resultLiveData = useCase.observe()

        useCase.execute(StarEventParameter("userIdTest", TestData.userEvents[0]))

        val result = LiveDataTestUtil.getValue(resultLiveData)
        assertTrue(result is Result.Error)
    }
}

val FailingSessionAndUserEventRepository = object : SessionAndUserEventRepository {
    val result = MutableLiveData<Result<StarUpdatedStatus>>()

    override fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): LiveData<Result<StarUpdatedStatus>> {
        result.postValue(Result.Error(Exception("Test")))
        return result
    }

    override fun getObservableUserEvents(
        userId: String?
    ): LiveData<Result<LoadUserSessionsByDayUseCaseResult>> {
        throw NotImplementedError()
    }

    override fun getObservableUserEvent(
        userId: String?,
        eventId: SessionId
    ): LiveData<Result<LoadUserSessionUseCaseResult>> {
        throw NotImplementedError()
    }

    override fun getUserEvents(userId: String?): List<UserEvent> {
        throw NotImplementedError()
    }

    override fun clearSingleEventSubscriptions() {}
}
