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

package com.google.samples.apps.iosched.ui.signin

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefSaveActionUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefShownActionUseCase
import dagger.android.support.DaggerAppCompatDialogFragment
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Dialog that asks for the user's notifications preference.
 */
class NotificationsPreferenceDialogFragment : DaggerAppCompatDialogFragment() {

    @Inject
    lateinit var notificationsPrefSaveActionUseCase: NotificationsPrefSaveActionUseCase
    @Inject
    lateinit var notificationsPrefShownActionUseCase: NotificationsPrefShownActionUseCase

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.notifications_preference_dialog_title)
            .setMessage(R.string.notifications_preference_dialog_content)
            .setNegativeButton(R.string.no) { _, _ ->
                lifecycleScope.launch {
                    notificationsPrefSaveActionUseCase(false)
                }
            }
            .setPositiveButton(R.string.yes) { _, _ ->
                lifecycleScope.launch {
                    notificationsPrefSaveActionUseCase(true)
                }
            }
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        notificationsPrefShownActionUseCase(true)
        super.onDismiss(dialog)
    }

    companion object {
        const val DIALOG_NOTIFICATIONS_PREFERENCE = "dialog_notifications_preference"
    }
}
