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
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnLayout
import androidx.core.view.doOnNextLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.ScheduleDetailNavGraphDirections
import com.google.samples.apps.iosched.databinding.FragmentScheduleTwoPaneBinding
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.messages.setupSnackbarManager
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.util.launchAndRepeatWithViewLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduleTwoPaneFragment : MainNavigationFragment() {

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    private val scheduleTwoPaneViewModel: ScheduleTwoPaneViewModel by activityViewModels()

    private lateinit var binding: FragmentScheduleTwoPaneBinding

    private lateinit var listPaneNavController: NavController
    private lateinit var detailPaneNavController: NavController

    private val backPressHandler = BackPressHandler()

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

        view.doOnLayout {
            activity?.reportFullyDrawn()
        }

        setupSnackbarManager(snackbarMessageManager, binding.snackbar)

        binding.slidingPaneLayout.apply {
            // Disable dragging the detail pane.
            lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED
            // Listen for movement of the detail pane.
            addPanelSlideListener(backPressHandler)
        }

        childFragmentManager.run {
            listPaneNavController =
                (findFragmentById(R.id.list_pane) as NavHostFragment).navController
            detailPaneNavController =
                (findFragmentById(R.id.detail_pane) as NavHostFragment).navController
            listPaneNavController.addOnDestinationChangedListener(backPressHandler)
            detailPaneNavController.addOnDestinationChangedListener(backPressHandler)
        }

        binding.slidingPaneLayout.doOnNextLayout {
            scheduleTwoPaneViewModel.setIsTwoPane(!binding.slidingPaneLayout.isSlideable)
        }

        launchAndRepeatWithViewLifecycle {
            launch {
                scheduleTwoPaneViewModel.selectSessionEvents.collect { sessionId ->
                    detailPaneNavController.navigate(
                        ScheduleDetailNavGraphDirections.toSessionDetail(sessionId)
                    )
                    // On narrow screens, slide the detail pane over the list pane if it isn't already
                    // on top. If both panes are visible, this will have no effect.
                    binding.slidingPaneLayout.open()
                }
            }
            launch {
                scheduleTwoPaneViewModel.navigateToSignInDialogEvents.collect {
                    openSignInDialog()
                }
            }
            launch {
                scheduleTwoPaneViewModel.returnToListPaneEvents.collect {
                    binding.slidingPaneLayout.close()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressHandler)
    }

    // TODO convert this to a dialog destination in the nav graph
    private fun openSignInDialog() {
        val dialog = SignInDialogFragment()
        dialog.show(requireActivity().supportFragmentManager, SignInDialogFragment.DIALOG_SIGN_IN)
    }

    /** Handles back button press while this fragment is on screen. */
    inner class BackPressHandler :
        OnBackPressedCallback(false),
        SlidingPaneLayout.PanelSlideListener,
        NavController.OnDestinationChangedListener {

        override fun handleOnBackPressed() {
            // Back press can have three possible effects that we check for in order.
            // 1. In the detail pane, go back from Speaker Detail to Session Detail.
            val listDestination = listPaneNavController.currentDestination?.id
            val detailDestination = detailPaneNavController.currentDestination?.id
            var done = false
            if (detailDestination == R.id.navigation_speaker_detail) {
                done = detailPaneNavController.popBackStack()
            }
            // 2. On narrow screens, if the detail pane is in front, "go back" by sliding it away.
            if (!done) {
                done = binding.slidingPaneLayout.closePane()
            }
            // 3. Try to pop the list pane, e.g. back from Search to Schedule.
            if (!done && listDestination == R.id.navigation_schedule_search) {
                listPaneNavController.popBackStack()
            }

            syncEnabledState()
        }

        override fun onPanelSlide(panel: View, slideOffset: Float) {
            // noop
        }

        override fun onPanelOpened(panel: View) {
            syncEnabledState()
        }

        override fun onPanelClosed(panel: View) {
            syncEnabledState()
        }

        override fun onDestinationChanged(
            controller: NavController,
            destination: NavDestination,
            arguments: Bundle?
        ) {
            syncEnabledState()
        }

        private fun syncEnabledState() {
            val listDestination = listPaneNavController.currentDestination?.id
            val detailDestination = detailPaneNavController.currentDestination?.id
            isEnabled = listDestination == R.id.navigation_schedule_search ||
                detailDestination == R.id.navigation_speaker_detail ||
                (binding.slidingPaneLayout.isSlideable && binding.slidingPaneLayout.isOpen)
        }
    }
}
