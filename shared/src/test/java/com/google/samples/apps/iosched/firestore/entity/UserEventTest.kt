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

package com.google.samples.apps.iosched.firestore.entity

import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.test.data.TestData
import org.junit.Assert.assertTrue
import org.junit.Test

class UserEventTest {

    private fun createTestEvent(
        isStarred: Boolean = false
    ): UserEvent {
        return TestData.userEvents[0].copy(
            isStarred = isStarred
        )
    }

    @Test
    fun starred_isPinned() {
        val userEvent = createTestEvent(isStarred = true)
        assertTrue(userEvent.isPinned())
    }

    @Test
    fun notStarred_isNotPinned() {
        val notStarred = createTestEvent(isStarred = false)
        assertTrue(!notStarred.isPinned())
    }
}
