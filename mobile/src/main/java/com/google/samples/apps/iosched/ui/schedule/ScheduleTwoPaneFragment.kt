/*
 * Copyright 2021 Google LLC
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

package com.google.samples.apps.iosched.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.ScheduleDetailNavGraphDirections
import com.google.samples.apps.iosched.databinding.FragmentScheduleTwoPaneBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.messages.setupSnackbarManager
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleTwoPaneFragment : MainNavigationFragment() {

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    private val scheduleTwoPaneViewModel: ScheduleTwoPaneViewModel by activityViewModels()

    private lateinit var binding: FragmentScheduleTwoPaneBinding

    private lateinit var listPaneNavController: NavController
    private lateinit var detailPaneNavController: NavController

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScheduleTwoPaneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSnackbarManager(snackbarMessageManager, binding.snackbar)

        childFragmentManager.run {
            listPaneNavController =
                (findFragmentById(R.id.list_pane) as NavHostFragment).navController
            detailPaneNavController =
                (findFragmentById(R.id.detail_pane) as NavHostFragment).navController
        }

        scheduleTwoPaneViewModel.navigateToSessionAction.observe(
            viewLifecycleOwner,
            EventObserver { sessionId ->
                detailPaneNavController.navigate(
                    ScheduleDetailNavGraphDirections.toSessionDetail(sessionId)
                )
            }
        )

        scheduleTwoPaneViewModel.navigateToSignInDialogAction.observe(
            viewLifecycleOwner,
            EventObserver {
                openSignInDialog()
            }
        )
    }

    // TODO convert this to a dialog destination in the nav graph
    private fun openSignInDialog() {
        val dialog = SignInDialogFragment()
        dialog.show(requireActivity().supportFragmentManager, SignInDialogFragment.DIALOG_SIGN_IN)
    }
}
