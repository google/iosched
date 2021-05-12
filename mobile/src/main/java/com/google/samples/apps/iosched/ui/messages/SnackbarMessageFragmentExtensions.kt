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

import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.fragment.app.Fragment
import com.google.samples.apps.iosched.util.launchAndRepeatWithViewLifecycle
import com.google.samples.apps.iosched.widget.FadingSnackbar
import kotlinx.coroutines.flow.collect

/**
 * An extension for Fragments that sets up a Snackbar with a [SnackbarMessageManager].
 */
fun Fragment.setupSnackbarManager(
    snackbarMessageManager: SnackbarMessageManager,
    fadingSnackbar: FadingSnackbar
) {
    launchAndRepeatWithViewLifecycle {
        snackbarMessageManager.currentSnackbar.collect { message ->
            if (message == null) { return@collect }
            val messageText = HtmlCompat.fromHtml(
                requireContext().getString(message.messageId, message.session?.title),
                FROM_HTML_MODE_LEGACY
            )
            fadingSnackbar.show(
                messageText = messageText,
                actionId = message.actionId,
                longDuration = message.longDuration,
                actionClick = {
                    snackbarMessageManager.processDismissedMessage(message)
                    fadingSnackbar.dismiss()
                },
                // When the snackbar is dismissed, ping the snackbar message manager in case there
                // are pending messages.
                dismissListener = {
                    snackbarMessageManager.removeMessageAndLoadNext(message)
                }
            )
        }
    }
}
