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

package com.google.samples.apps.iosched.shared.domain.filters

import com.google.samples.apps.iosched.model.filters.Filter
import com.google.samples.apps.iosched.model.filters.Filter.DateFilter
import com.google.samples.apps.iosched.model.filters.Filter.MyScheduleFilter
import com.google.samples.apps.iosched.model.filters.Filter.TagFilter
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.test.data.TestData
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class UserSessionFilterMatcherTest {

    private fun assertMatchedSessions(
        filters: List<Filter>,
        expectedSessions: List<UserSession>
    ) {
        val matcher = UserSessionFilterMatcher(filters)
        val sessions = TestData.userSessionList.filter {
            matcher.matches(it)
        }
        assertThat(sessions, `is`(equalTo(expectedSessions)))
    }

    @Test
    fun `no filters matches all`() = assertMatchedSessions(
        filters = emptyList(),
        expectedSessions = TestData.userSessionList
    )

    @Test
    fun `filter by date`() = assertMatchedSessions(
        filters = listOf(DateFilter(TestData.TestConferenceDays[0])),
        expectedSessions = listOf(TestData.userSession0, TestData.userSession1)
    )

    @Test
    fun `filter by multiple dates`() = assertMatchedSessions(
        filters = listOf(
            DateFilter(TestData.TestConferenceDays[0]),
            DateFilter(TestData.TestConferenceDays[1])
        ),
        expectedSessions = listOf(
            TestData.userSession0,
            TestData.userSession1,
            TestData.userSession2
        )
    )

    @Test
    fun `filter by tag`() = assertMatchedSessions(
        filters = listOf(TagFilter(TestData.cloudTag)),
        expectedSessions = listOf(TestData.userSession1)
    )

    @Test
    fun `filter by multiple tags in same category`() = assertMatchedSessions(
        filters = listOf( // 2 topics
            TagFilter(TestData.cloudTag),
            TagFilter(TestData.webTag)
        ),
        expectedSessions = listOf(
            TestData.userSession0, // web
            TestData.userSession1, // cloud
            TestData.userSession3, // web
            TestData.userSession4 // cloud
        )
    )

    @Test
    fun `filter by tags in multiple categories`() = assertMatchedSessions(
        filters = listOf(
            TagFilter(TestData.androidTag), // topic
            TagFilter(TestData.sessionsTag) // type
        ),
        expectedSessions = listOf(TestData.userSession0, TestData.userSession2)
    )

    @Test
    fun `filter by my schedule`() = assertMatchedSessions(
        filters = listOf(MyScheduleFilter),
        expectedSessions = listOf(
            TestData.userSession0, TestData.userSession1, TestData.userSession2
        )
    )

    @Test
    fun `multiple filter types with match`() = assertMatchedSessions(
        filters = listOf(
            TagFilter(TestData.androidTag), // topic
            TagFilter(TestData.sessionsTag), // type
            MyScheduleFilter,
            DateFilter(TestData.TestConferenceDays[0])
        ),
        expectedSessions = listOf(TestData.userSession0)
    )

    @Test
    fun `multiple filter types no match`() = assertMatchedSessions(
        filters = listOf(
            TagFilter(TestData.androidTag), // topic
            TagFilter(TestData.sessionsTag), // type
            DateFilter(TestData.TestConferenceDays[2])
        ),
        expectedSessions = emptyList()
    )
}
