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

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager.Companion.MAX_ITEMS
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A single source of Snackbar messages related to reservations.
 *
 * Only shows one Snackbar related to one change across all screens
 *
 * Emits new values on request (when a Snackbar is dismissed and ready to show a new message)
 *
 * It keeps a list of [MAX_ITEMS] items, enough to figure out if a message has already been shown,
 * but limited to avoid wasting resources.
 *
 */
@Singleton
open class SnackbarMessageManager @Inject constructor(
    private val preferenceStorage: PreferenceStorage
) {
    companion object {
        // Keep a fixed number of old items
        @VisibleForTesting
        const val MAX_ITEMS = 10
    }

    private val messages = mutableListOf<Event<SnackbarMessage>>()

    private val result = MutableLiveData<Event<SnackbarMessage>>()

    fun addMessage(msg: SnackbarMessage) {
        if (shouldSnackbarBeIgnored(msg)) {
            return
        }
        // If the new message is about the same change as a pending one, keep the new one. (rare)
        val sameRequestId = messages.filter {
            it.peekContent().requestChangeId == msg.requestChangeId && !it.hasBeenHandled
        }
        if (sameRequestId.isNotEmpty()) {
            messages.removeAll(sameRequestId)
        }

        // If the new message is about a change that was already notified, ignore it.
        val alreadyHandledWithSameId = messages.filter {
            it.peekContent().requestChangeId == msg.requestChangeId && it.hasBeenHandled
        }

        // Only add the message if it hasn't been handled before
        if (alreadyHandledWithSameId.isEmpty()) {
            messages.add(Event(msg))
            loadNextMessage()
        }

        // Remove old messages
        if (messages.size > MAX_ITEMS) {
            messages.retainAll(messages.drop(messages.size - MAX_ITEMS))
        }
    }

    fun loadNextMessage() {
        result.postValue(messages.firstOrNull { !it.hasBeenHandled })
    }

    fun observeNextMessage(): MutableLiveData<Event<SnackbarMessage>> {
        return result
    }

    private fun shouldSnackbarBeIgnored(msg: SnackbarMessage): Boolean {
        return preferenceStorage.observableSnackbarIsStopped.value == true &&
            msg.actionId == R.string.dont_show
    }
}
