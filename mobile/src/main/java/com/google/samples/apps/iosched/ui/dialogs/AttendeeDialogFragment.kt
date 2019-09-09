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

package com.google.samples.apps.iosched.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import dagger.android.support.DaggerAppCompatDialogFragment
import javax.inject.Inject

/**
 * Dialog that asks how the user is attending the conference.
 */
class AttendeeDialogFragment : DaggerAppCompatDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: AttendeePreferenceViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewModel = viewModelProvider(viewModelFactory)
        viewModel.dismissDialogEvent.observe(this, EventObserver {
            dismiss()
        })

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.user_attendee_dialog_title)
            .setMessage(R.string.user_attendee_dialog_description)
            .setNegativeButton(
                R.string.user_attendee_dialog_remote
            ) { _, _ -> viewModel.onRemotelyClicked() }
            .setPositiveButton(
                R.string.user_attendee_dialog_in_person
            ) { _, _ -> viewModel.onInPersonClicked() }
            .create()
    }

    companion object {
        const val DIALOG_USER_ATTENDEE_PREFERENCE = "dialog_user_attendee_preference"
    }
}
