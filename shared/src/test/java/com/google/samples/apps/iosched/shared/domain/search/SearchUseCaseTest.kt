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

@file:Suppress(names = ["FunctionName"])

package com.google.samples.apps.iosched.shared.domain.search

import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.TestData
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsCollectionContaining.hasItem
import org.hamcrest.core.IsCollectionContaining.hasItems
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsInstanceOf.instanceOf
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class SearchUseCaseTest {

    private lateinit var useCase: SearchUseCase

    @Before
    fun setup() {
        useCase = SearchUseCase(DefaultSessionRepository(TestDataRepository))
    }

    @Test
    fun search_MatchesOnTitle() {
        val result = useCase.executeNow(parameters = "session 0")
        assertThatResultContainsOnlySession0(result)
    }

    @Test
    fun search_MatchesOnAbstract() {
        val result = useCase.executeNow(parameters = "Awesome")
        assertThatResultContainsOnlySession0(result)
    }

    @Test
    fun search_MatchesOnTagName() {
        val result = useCase.executeNow(parameters = "android")
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))

        val sessions = (result as Result.Success).data
        assertThat(sessions.size, `is`(equalTo(3)))
        assertThat(sessions, hasItems(TestData.session0, TestData.session1, TestData.session2))
    }

    @Test
    fun search_MatchesOnUserQuestion() {
        val result = useCase.executeNow(parameters = "What are the Android talks at Google I/O")
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))

        val sessions = (result as Result.Success).data
        assertThat(sessions.size, `is`(equalTo(3)))
        assertThat(sessions, hasItems(TestData.session0, TestData.session1, TestData.session2))
    }

    @Test
    fun search_returnsEmptyListForInvalidQuery() {
        val result = useCase.executeNow(parameters = "In valid search query")
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))

        val sessions = (result as Result.Success).data
        assertThat(sessions, `is`(equalTo(emptyList())))
    }

    private fun assertThatResultContainsOnlySession0(result: Result<List<Session>>) {
        assertThat(result, `is`(instanceOf(Result.Success::class.java)))

        val sessions = (result as Result.Success).data
        assertThat(sessions.size, `is`(equalTo(1)))
        assertThat(sessions, hasItem(TestData.session0))
    }
}
