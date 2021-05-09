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
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.domain.prefs.StopSnackbarActionUseCase
import com.google.samples.apps.iosched.test.data.CoroutineScope
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [SnackbarMessageManager].
 */
class SnackbarMessageManagerTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private fun createSnackbarMessageManager(
        preferenceStorage: PreferenceStorage = FakePreferenceStorage()
    ): SnackbarMessageManager {
        return SnackbarMessageManager(
            preferenceStorage,
            coroutineRule.CoroutineScope(),
            StopSnackbarActionUseCase(preferenceStorage, coroutineRule.testDispatcher)
        )
    }

    @Test
    fun addOneMessage() {
        val snackbarMessageManager = createSnackbarMessageManager()
        snackbarMessageManager.addMessage(msg1)

        val result = snackbarMessageManager.currentSnackbar.value

        assertEquals(result, msg1)
    }

    @Test
    fun addTwoMessages_OrderMaintained_NullWhenNoMore() {
        val snackbarMessageManager = createSnackbarMessageManager()

        snackbarMessageManager.addMessage(msg1)
        snackbarMessageManager.addMessage(msg2)

        // First message is consumed
        var result = snackbarMessageManager.currentSnackbar.value
        assertEquals(result, msg1)
        snackbarMessageManager.removeMessageAndLoadNext(msg1) // Snackbar dismissed

        // Second message is consumed
        result = snackbarMessageManager.currentSnackbar.value
        assertEquals(result, msg2)
        snackbarMessageManager.removeMessageAndLoadNext(msg2) // Snackbar dismissed

        // All messages have been consumed
        result = snackbarMessageManager.currentSnackbar.value
        assertNull(result)
    }

    @Test
    fun addTwoMessagesSameRequestId_OnlyOneShows() {
        val snackbarMessageManager = createSnackbarMessageManager()

        snackbarMessageManager.addMessage(msg1)
        snackbarMessageManager.addMessage(msg1)

        // First message is consumed
        var result = snackbarMessageManager.currentSnackbar.value
        assertEquals(result, msg1)
        snackbarMessageManager.removeMessageAndLoadNext(msg1) // Snackbar dismissed

        // All messages have been consumed
        result = snackbarMessageManager.currentSnackbar.value
        assertNull(result)
    }

    @Test
    fun addMessagesToQueue_NewOnesRemoved() {
        val snackbarMessageManager = createSnackbarMessageManager()

        val addedMsgs = 15
        (0..addedMsgs).forEach {
            val newMsg = createMessage(it.toString())
            snackbarMessageManager.addMessage(newMsg)
        }

        val result = snackbarMessageManager.currentSnackbar.value
        assertEquals(result?.requestChangeId, 0.toString())

        (0 until SnackbarMessageManager.MAX_ITEMS).forEach {
            val newMsg = createMessage(it.toString())
            snackbarMessageManager.removeMessageAndLoadNext(newMsg)
        }

        // The last message request ID should be 10, because we added 15 and the maximum is 10.
        val lastMsg = snackbarMessageManager.currentSnackbar.value
        val lastId = SnackbarMessageManager.MAX_ITEMS.toString()
        assertEquals(lastMsg?.requestChangeId, lastId)
    }

    @Test
    fun addOneMessage_snackbarIsStopped_actionDontShow() {
        val snackbarMessageManager = createSnackbarMessageManager(
            FakePreferenceStorage(snackbarIsStopped = true)
        )
        snackbarMessageManager.addMessage(msg1.copy(actionId = R.string.dont_show))

        val result = snackbarMessageManager.currentSnackbar.value
        assertNull(result)
    }

    @Test
    fun addOneMessage_snackbarAppears_actionNotDontShow() {
        val snackbarMessageManager = createSnackbarMessageManager(
            FakePreferenceStorage(snackbarIsStopped = true)
        )
        snackbarMessageManager.addMessage(msg1)

        val result = snackbarMessageManager.currentSnackbar.value
        assertEquals(result, msg1)
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
