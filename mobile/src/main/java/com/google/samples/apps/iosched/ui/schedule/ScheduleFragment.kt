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

package com.google.samples.apps.iosched.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnNextLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleBinding
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.prefs.SnackbarPreferenceViewModel
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailActivity
import com.google.samples.apps.iosched.ui.setUpSnackbar
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment.Companion.DIALOG_NOTIFICATIONS_PREFERENCE
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignOutDialogFragment
import com.google.samples.apps.iosched.util.clearDecorations
import com.google.samples.apps.iosched.util.executeAfter
import com.google.samples.apps.iosched.util.fabVisibility
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_COLLAPSED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_EXPANDED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_HIDDEN
import com.google.samples.apps.iosched.widget.FadingSnackbar
import javax.inject.Inject
import javax.inject.Named

/**
 * The Schedule page of the top-level Activity.
 */
class ScheduleFragment : MainNavigationFragment() {

    companion object {
        private const val DIALOG_NEED_TO_SIGN_IN = "dialog_need_to_sign_in"
        private const val DIALOG_CONFIRM_SIGN_OUT = "dialog_confirm_sign_out"
        private const val DIALOG_SCHEDULE_HINTS = "dialog_schedule_hints"
    }

    @Inject lateinit var analyticsHelper: AnalyticsHelper

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    @field:Named("tagViewPool")
    lateinit var tagViewPool: RecycledViewPool

    @Inject lateinit var snackbarMessageManager: SnackbarMessageManager

    private lateinit var scheduleViewModel: ScheduleViewModel

    private lateinit var filtersFab: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var snackbar: FadingSnackbar

    private lateinit var adapter: ScheduleAdapter

    private lateinit var binding: FragmentScheduleBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // ViewModel shared with child fragments.
        scheduleViewModel = viewModelProvider(viewModelFactory)
        binding = FragmentScheduleBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@ScheduleFragment.scheduleViewModel
        }

        filtersFab = binding.filterFab
        snackbar = binding.snackbar
        recyclerView = binding.recyclerview
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Snackbar configuration
        val snackbarPrefViewModel: SnackbarPreferenceViewModel = viewModelProvider(viewModelFactory)
        setUpSnackbar(scheduleViewModel.snackBarMessage, snackbar, snackbarMessageManager,
            actionClickListener = {
                snackbarPrefViewModel.onStopClicked()
            }
        )

        // Filters sheet configuration
        // This view is not in the Binding class because it's in an included layout.
        val appbar: View = view.findViewById(R.id.appbar)
        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.filter_sheet))
        filtersFab.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val a11yState = if (newState == STATE_EXPANDED) {
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                } else {
                    View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                }
                recyclerView.importantForAccessibility = a11yState
                appbar.importantForAccessibility = a11yState
            }
        })

        // Session list configuration
        adapter = ScheduleAdapter(
            scheduleViewModel,
            tagViewPool,
            scheduleViewModel.showReservations,
            scheduleViewModel.timeZoneId,
            this
        )
        recyclerView.apply {
            adapter = this@ScheduleFragment.adapter
            (layoutManager as LinearLayoutManager).recycleChildrenOnDetach = true
            (itemAnimator as DefaultItemAnimator).run {
                supportsChangeAnimations = false
                addDuration = 160L
                moveDuration = 160L
                changeDuration = 160L
                removeDuration = 120L
            }
        }

        // TODO(b/124072759) UI affordance to jump to start of each day

        // Start observing ViewModels
        scheduleViewModel.sessionTimeData.observe(this, Observer {
            it ?: return@Observer
            initializeList(it)
        })

        // During conference, scroll to current event.
        scheduleViewModel.currentEvent.observe(this, Observer { index ->
            if (index > -1 && !scheduleViewModel.userHasInteracted) {
                recyclerView.run {
                    post {
                        (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            index,
                            resources.getDimensionPixelSize(R.dimen.margin_normal)
                        )
                    }
                }
            }
        })

        scheduleViewModel.navigateToSessionAction.observe(this, EventObserver { sessionId ->
            openSessionDetail(sessionId)
        })

        scheduleViewModel.navigateToSignInDialogAction.observe(this, EventObserver {
            openSignInDialog()
        })

        scheduleViewModel.navigateToSignOutDialogAction.observe(this, EventObserver {
            openSignOutDialog()
        })
        scheduleViewModel.scheduleUiHintsShown.observe(this, EventObserver {
            if (!it) {
                openScheduleUiHintsDialog()
            }
        })
        scheduleViewModel.shouldShowNotificationsPrefAction.observe(this, EventObserver {
            if (it) {
                openNotificationsPreferenceDialog()
            }
        })
        scheduleViewModel.hasAnyFilters.observe(this, Observer {
            updateFiltersUi(it ?: return@Observer)
        })

        // Show an error message
        scheduleViewModel.errorMessage.observe(this, EventObserver { errorMsg ->
            // TODO: Change once there's a way to show errors to the user
            Toast.makeText(this.context, errorMsg, Toast.LENGTH_LONG).show()
        })

        if (savedInstanceState == null) {
            // VM outlives the UI, so reset this flag when a new Schedule page is shown
            scheduleViewModel.userHasInteracted = false
        }
        analyticsHelper.sendScreenView("Schedule", requireActivity())
    }

    private fun initializeList(sessionTimeData: SessionTimeData) {
        // Require the list and timeZoneId to be loaded.
        val list = sessionTimeData.list ?: return
        val timeZoneId = sessionTimeData.timeZoneId ?: return
        adapter.submitList(list)

        binding.recyclerview.run {
            // we want this to run after diffing
            doOnNextLayout { view ->
                // Recreate the decoration used for the sticky time headers
                clearDecorations()
                if (list.isNotEmpty()) {
                    addItemDecoration(
                        ScheduleTimeHeadersDecoration(
                            view.context, list.map { it.session }, timeZoneId
                        )
                    )
                }
            }
        }

        binding.executeAfter {
            isEmpty = list.isEmpty()
        }
    }

    private fun updateFiltersUi(hasAnyFilters: Boolean) {
        val showFab = !hasAnyFilters

        fabVisibility(filtersFab, showFab)
        // Set snackbar position depending whether fab/filters show.
        snackbar.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            bottomMargin = resources.getDimensionPixelSize(
                if (showFab) {
                    R.dimen.snackbar_margin_bottom_fab
                } else {
                    R.dimen.bottom_sheet_peek_height
                }
            )
        }
        bottomSheetBehavior.isHideable = showFab
        bottomSheetBehavior.skipCollapsed = showFab
        if (showFab && bottomSheetBehavior.state == STATE_COLLAPSED) {
            bottomSheetBehavior.state = STATE_HIDDEN
        }
    }

    override fun onBackPressed(): Boolean {
        if (::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state == STATE_EXPANDED) {
            // collapse or hide the sheet
            if (bottomSheetBehavior.isHideable && bottomSheetBehavior.skipCollapsed) {
                bottomSheetBehavior.state = STATE_HIDDEN
            } else {
                bottomSheetBehavior.state = STATE_COLLAPSED
            }
            return true
        }
        return super.onBackPressed()
    }

    override fun onUserInteraction() {
        // Guard against a crash.
        // Rarely observed the method was called before the ViewModel was initialized.
        if (::scheduleViewModel.isInitialized) {
            scheduleViewModel.userHasInteracted = true
        }
    }

    private fun openSessionDetail(id: SessionId) {
        startActivity(SessionDetailActivity.starterIntent(requireContext(), id))
    }

    private fun openSignInDialog() {
        val dialog = SignInDialogFragment()
        dialog.show(requireActivity().supportFragmentManager, DIALOG_NEED_TO_SIGN_IN)
    }

    private fun openSignOutDialog() {
        val dialog = SignOutDialogFragment()
        dialog.show(requireActivity().supportFragmentManager, DIALOG_CONFIRM_SIGN_OUT)
    }

    private fun openScheduleUiHintsDialog() {
        val dialog = ScheduleUiHintsDialogFragment()
        dialog.show(requireActivity().supportFragmentManager, DIALOG_SCHEDULE_HINTS)
    }

    private fun openNotificationsPreferenceDialog() {
        val dialog = NotificationsPreferenceDialogFragment()
        dialog.show(requireActivity().supportFragmentManager, DIALOG_NOTIFICATIONS_PREFERENCE)
    }

    override fun onStart() {
        super.onStart()
        scheduleViewModel.initializeTimeZone()
    }
}
