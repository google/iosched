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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.samples.apps.iosched.databinding.DialogSwapReservationBinding
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestParameters
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.widget.CustomDimDialogFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

/**
 * Dialog that confirms the user that if the user wants to replace their reservations
 */
class SwapReservationDialogFragment : CustomDimDialogFragment(), HasSupportFragmentInjector {

    companion object {
        const val DIALOG_SWAP_RESERVATION = "dialog_replace_reservation"
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
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var swapViewModel: SwapReservationViewModel

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentInjector
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        swapViewModel = viewModelProvider(viewModelFactory)

        requireNotNull(arguments).run {
            swapViewModel.userId = getString(USER_ID_KEY)
            swapViewModel.fromId = getString(FROM_ID_KEY)
            swapViewModel.fromTitle = getString(FROM_TITLE_KEY)
            swapViewModel.toId = getString(TO_ID_KEY)
            swapViewModel.toTitle = getString(TO_TITLE_KEY)
        }

        val binding = DialogSwapReservationBinding.inflate(inflater, container, false).apply {
            viewModel = swapViewModel
        }
        swapViewModel.dismissDialogAction.observe(this, EventObserver {
            dismiss()
        })
        return binding.root
    }
}
