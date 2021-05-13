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
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.di.ApplicationScope
import com.google.samples.apps.iosched.shared.domain.prefs.StopSnackbarActionUseCase
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager.Companion.MAX_ITEMS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val preferenceStorage: PreferenceStorage,
    @ApplicationScope private val coroutineScope: CoroutineScope,
    private val stopSnackbarActionUseCase: StopSnackbarActionUseCase
) {
    companion object {
        // Keep a fixed number of old items
        @VisibleForTesting
        const val MAX_ITEMS = 10
    }

    private val messages = mutableListOf<SnackbarMessage>()

    private val _currentSnackbar = MutableStateFlow<SnackbarMessage?>(null)
    val currentSnackbar: StateFlow<SnackbarMessage?> = _currentSnackbar

    fun addMessage(msg: SnackbarMessage) {
        coroutineScope.launch {
            if (!shouldSnackbarBeIgnored(msg)) {
                // Limit amount of pending messages
                if (messages.size > MAX_ITEMS) {
                    Timber.e("Too many Snackbar messages. Message id: ${msg.messageId}")
                    return@launch
                }
                // If the new message is about the same change as a pending one, keep the old one. (rare)
                val sameRequestId = messages.find {
                    it.requestChangeId == msg.requestChangeId
                }
                if (sameRequestId == null) {
                    messages.add(msg)
                }
                loadNext()
            }
        }
    }

    private fun loadNext() {
        if (_currentSnackbar.value == null) {
            _currentSnackbar.value = messages.firstOrNull()
        }
    }

    fun removeMessageAndLoadNext(shownMsg: SnackbarMessage?) {
        messages.removeAll { it == shownMsg }
        if (_currentSnackbar.value == shownMsg) {
            _currentSnackbar.value = null
        }
        loadNext()
    }

    fun processDismissedMessage(message: SnackbarMessage) {
        if (message.actionId == R.string.dont_show) {
            coroutineScope.launch {
                stopSnackbarActionUseCase(true)
            }
        }
    }

    private suspend fun shouldSnackbarBeIgnored(msg: SnackbarMessage): Boolean {
        return preferenceStorage.isSnackbarStopped() && msg.actionId == R.string.dont_show
    }
}
