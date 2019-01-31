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

package com.google.samples.apps.iosched.ui.messages

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.ui.SnackbarMessage
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [SnackbarMessageManager].
 */
class SnackbarMessageManagerTest {

    private lateinit var snackbarMessageManager: SnackbarMessageManager

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Before
    fun createSubject() {
        snackbarMessageManager = SnackbarMessageManager(FakePreferenceStorage())
    }

    @Test
    fun addOneMessage() {
        snackbarMessageManager.addMessage(msg1)

        val result = LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())

        assertThat(result?.peekContent(), `is`(equalTo(msg1)))
    }

    @Test
    fun addTwoMessages_OrderMaintained_NullWhenNoMore() {

        snackbarMessageManager.addMessage(msg1)

        snackbarMessageManager.addMessage(msg2)

        // First message is consumed
        var result = LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        assertThat(result?.getContentIfNotHandled(), `is`(equalTo(msg1)))
        snackbarMessageManager.loadNextMessage() // Snackbar dismissed

        // Second message is consumed
        result = LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        assertThat(result?.getContentIfNotHandled(), `is`(equalTo(msg2)))
        snackbarMessageManager.loadNextMessage() // Snackbar dismissed

        // All messages have been consumed
        result = LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        assertThat(result?.getContentIfNotHandled(), `is`(nullValue()))
    }

    @Test
    fun addTwoMessagesSameRequestId_OnlyOneShows() {

        snackbarMessageManager.addMessage(msg1)

        snackbarMessageManager.addMessage(msg1)

        // First message is consumed
        var result = LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        assertThat(result?.getContentIfNotHandled(), `is`(equalTo(msg1)))
        snackbarMessageManager.loadNextMessage() // Snackbar dismissed

        // All messages have been consumed
        result = LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        assertThat(result?.getContentIfNotHandled(), `is`(nullValue()))
    }

    @Test
    fun addMessagesToQueue_OldOnesRemoved() {
        val addedMsgs = 15
        (0..addedMsgs).forEach {
            val newMsg = createMessage(it.toString())
            snackbarMessageManager.addMessage(newMsg)
        }

        val result = LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())

        // The oldest message request ID should be 5, because we added 15 and the maximum is 10.
        val oldestId = (addedMsgs - SnackbarMessageManager.MAX_ITEMS).toString()
        assertThat(result?.getContentIfNotHandled()?.requestChangeId, `is`(equalTo(oldestId)))
    }

    @Test
    fun addOneMessage_snackbarIsStopped_actionDontShow() {
        val snackbarMessageManager = SnackbarMessageManager(
            FakePreferenceStorage().apply { snackbarIsStopped = true }
        )
        snackbarMessageManager.addMessage((msg1.copy(actionId = R.string.dont_show)))

        val result = LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        assertNull(result)
    }

    @Test
    fun addOneMessage_snackbarAppears_actionNotDontShow() {
        val snackbarMessageManager = SnackbarMessageManager(
            FakePreferenceStorage().apply { snackbarIsStopped = true }
        )
        snackbarMessageManager.addMessage(msg1)

        val result = LiveDataTestUtil.getValue(snackbarMessageManager.observeNextMessage())
        assertThat(result?.peekContent(), `is`(equalTo(msg1)))
    }

    private fun createMessage(requestId: String): SnackbarMessage {
        return SnackbarMessage(
            messageId = 100,
            actionId = 500,
            requestChangeId = requestId,
            session = TestData.session0
        )
    }
}

val msg1 = SnackbarMessage(
    messageId = 123,
    actionId = 321,
    requestChangeId = "42",
    session = TestData.session0
)

val msg2 = SnackbarMessage(
    messageId = 123,
    actionId = 321,
    requestChangeId = "43",
    session = TestData.session1
)
