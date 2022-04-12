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
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.lifecycle.coroutineScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.domain.users.SwapActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestParameters
import com.google.samples.apps.iosched.util.makeBold
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dialog that confirms the user wants to replace their reservations
 */
@AndroidEntryPoint
class SwapReservationDialogFragment : AppCompatDialogFragment() {

    companion object {
        const val DIALOG_SWAP_RESERVATION = "dialog_swap_reservation"
        private const val USER_ID_KEY = "user_id"
        private const val FROM_ID_KEY = "from_id"
        private const val FROM_TITLE_KEY = "from_title"
        private const val TO_ID_KEY = "to_id"
        private const val TO_TITLE_KEY = "to_title"

        fun newInstance(parameters: SwapRequestParameters): SwapReservationDialogFragment {
            val bundle = Bundle().apply {
                putString(USER_ID_KEY, parameters.userId)
                putString(FROM_ID_KEY, parameters.fromId)
                putString(FROM_TITLE_KEY, parameters.fromTitle)
                putString(TO_ID_KEY, parameters.toId)
                putString(TO_TITLE_KEY, parameters.toTitle)
            }
            return SwapReservationDialogFragment().apply { arguments = bundle }
        }
    }

    @Inject
    lateinit var swapActionUseCase: SwapActionUseCase

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val args = requireNotNull(arguments)
        val userId = requireNotNull(args.getString(USER_ID_KEY))
        val fromId = requireNotNull(args.getString(FROM_ID_KEY))
        val fromTitle = requireNotNull(args.getString(FROM_TITLE_KEY))
        val toId = requireNotNull(args.getString(TO_ID_KEY))
        val toTitle = requireNotNull(args.getString(TO_TITLE_KEY))

        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.swap_reservation_title)
            .setMessage(formatSwapReservationMessage(context.resources, fromTitle, toTitle))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.swap) { _, _ ->
                viewLifecycleOwner.lifecycle.coroutineScope.launch {
                    swapActionUseCase(
                        SwapRequestParameters(userId, fromId, fromTitle, toId, toTitle)
                    )
                }
            }
            .create()
    }

    private fun formatSwapReservationMessage(
        res: Resources,
        fromTitle: String,
        toTitle: String
    ): CharSequence {
        val text = res.getString(R.string.swap_reservation_content, fromTitle, toTitle)
        return text.makeBold(fromTitle).makeBold(toTitle)
    }
}
