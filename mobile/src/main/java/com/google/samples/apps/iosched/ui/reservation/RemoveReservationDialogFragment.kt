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

package com.google.samples.apps.iosched.ui.reservation

import android.app.Dialog
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.util.makeBold
import dagger.android.support.DaggerAppCompatDialogFragment
import javax.inject.Inject

/**
 * Dialog that confirms the user really wants to cancel their reservation
 */
class RemoveReservationDialogFragment : DaggerAppCompatDialogFragment() {

    companion object {
        const val DIALOG_REMOVE_RESERVATION = "dialog_remove_reservation"
        private const val USER_ID_KEY = "user_id"
        private const val SESSION_ID_KEY = "session_id"
        private const val SESSION_TITLE_KEY = "session_title"

        fun newInstance(
            parameters: RemoveReservationDialogParameters
        ): RemoveReservationDialogFragment {
            val bundle = Bundle().apply {
                putString(USER_ID_KEY, parameters.userId)
                putString(SESSION_ID_KEY, parameters.sessionId)
                putString(SESSION_TITLE_KEY, parameters.sessionTitle)
            }
            return RemoveReservationDialogFragment().apply { arguments = bundle }
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: RemoveReservationViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val args = requireNotNull(arguments)
        val sessionTitle = requireNotNull(args.getString(SESSION_TITLE_KEY))

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.remove_reservation_title)
            .setMessage(formatRemoveReservationMessage(context.resources, sessionTitle))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.remove) { _, _ ->
                viewModel.removeReservation()
            }
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = viewModelProvider(viewModelFactory)
        val sessionId = arguments?.getString(SESSION_ID_KEY)
        if (sessionId == null) {
            dismiss()
        } else {
            viewModel.setSessionId(sessionId)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun formatRemoveReservationMessage(
        res: Resources,
        sessionTitle: String
    ): CharSequence {
        val text = res.getString(R.string.remove_reservation_content, sessionTitle)
        return text.makeBold(sessionTitle)
    }
}

data class RemoveReservationDialogParameters(
    val userId: String,
    val sessionId: SessionId,
    val sessionTitle: String
)
