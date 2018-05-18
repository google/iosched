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

package com.google.samples.apps.iosched.wear.ui.sessiondetail

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.samples.apps.iosched.wear.databinding.FragmentSessionDetailBinding
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import dagger.android.support.DaggerFragment
import javax.inject.Inject

/**
 * Displays session details relevant for Wearable devices.
 */
class SessionDetailFragment : DaggerFragment() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var sessionDetailViewModel: SessionDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sessionDetailViewModel = viewModelProvider(viewModelFactory)
        sessionDetailViewModel.loadSessionById(
                requireNotNull(arguments).getString(EXTRA_SESSION_ID))

        val binding = FragmentSessionDetailBinding.inflate(inflater, container, false).apply {
            viewModel = sessionDetailViewModel
            setLifecycleOwner(this@SessionDetailFragment)
        }

        // TODO: Wear snackbars for errors based on VM info.

        return binding.root
    }

    companion object {
        private const val EXTRA_SESSION_ID = "SESSION_ID"

        fun newInstance(sessionId: String): SessionDetailFragment {
            val bundle = Bundle().apply { putString(EXTRA_SESSION_ID, sessionId) }
            return SessionDetailFragment().apply { arguments = bundle }
        }
    }
}
