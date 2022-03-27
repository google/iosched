/*
 * Copyright 2020 Google LLC
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

package com.google.samples.apps.iosched.shared.domain.search

import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.FakeSearchAppDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@ExperimentalCoroutinesApi
@RunWith(Parameterized::class)
class SessionTextMatchStrategyTest(private val strategy: SessionTextMatchStrategy) {

    companion object {
        val coroutineRule = MainCoroutineRule()

        @JvmStatic
        @Parameterized.Parameters
        fun useCases() = listOf(
            arrayOf(SimpleMatchStrategy),
            arrayOf(FtsMatchStrategy(FakeSearchAppDatabase()))
        )
    }

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = Companion.coroutineRule

    @Test
    fun `match on title`() = assertSearchResults(
        FakeSearchAppDatabase.QUERY_TITLE,
        listOf(TestData.userSession0)
    )

    @Test
    fun `match on description`() = assertSearchResults(
        FakeSearchAppDatabase.QUERY_ABSTRACT,
        listOf(TestData.userSession0)
    )

    @Test
    fun `no match returns empty list`() = assertSearchResults(
        FakeSearchAppDatabase.QUERY_WITH_NO_MATCH,
        emptyList()
    )

    @Test
    fun `blank query returns empty list`() = assertSearchResults(
        FakeSearchAppDatabase.QUERY_ONLY_SPACES,
        emptyList()
    )

    @Test
    fun `empty query returns all`() = assertSearchResults("", TestData.userSessionList)

    private fun assertSearchResults(
        query: String,
        expectedSessions: List<UserSession>
    ) = runTest {
        val sessions = strategy.searchSessions(TestData.userSessionList, query)
        assertThat(sessions, `is`(equalTo(expectedSessions)))
    }
}
