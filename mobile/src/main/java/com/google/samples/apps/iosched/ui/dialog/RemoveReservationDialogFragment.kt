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

package com.google.samples.apps.iosched.ui.dialog

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.DialogRemoveReservationBinding
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.widget.CustomDimDialogFragment
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.AndroidSupportInjection
import dagger.android.support.HasSupportFragmentInjector
import javax.inject.Inject

/**
 * Dialog that confirms the user that if the user really wants to cancel their reservation
 */
class RemoveReservationDialogFragment : CustomDimDialogFragment(), HasSupportFragmentInjector {

    companion object {
        const val DIALOG_REMOVE_RESERVATION = "dialog_remove_reservation"
        private const val USER_ID_KEY = "user_id"
        private const val SESSION_ID_KEY = "session_id"

        fun newInstance(parameters: ReservationRequestParameters): RemoveReservationDialogFragment {
            val bundle = Bundle().apply {
                putString(USER_ID_KEY, parameters.userId)
                putString(SESSION_ID_KEY, parameters.sessionId)
            }
            return RemoveReservationDialogFragment().apply { arguments = bundle }
        }
    }

    @Inject
    lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var removeViewModel: RemoveReservationViewModel
    private var userId: String? = null
    private var sessionId: String? = null

    override fun supportFragmentInjector(): AndroidInjector<Fragment> {
        return fragmentInjector
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString(USER_ID_KEY)
        sessionId = arguments?.getString(SESSION_ID_KEY)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        removeViewModel = viewModelProvider(viewModelFactory)
        val binding = DialogRemoveReservationBinding.inflate(inflater, container, false).apply {
            viewModel = removeViewModel
            userId = this@RemoveReservationDialogFragment.userId
            sessionId = this@RemoveReservationDialogFragment.sessionId
        }
        removeViewModel.dismissDialogAction.observe(this, Observer {
            it?.getContentIfNotHandled()?.let {
                dismiss()
            }
        })
        return binding.root
    }
}