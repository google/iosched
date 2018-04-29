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

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.support.design.widget.CoordinatorLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.view.updateLayoutParams
import com.google.android.material.widget.FloatingActionButton
import com.google.android.material.widget.TabLayout
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleBinding
import com.google.samples.apps.iosched.shared.model.SessionId
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.prefs.SnackbarPreferenceViewModel
import com.google.samples.apps.iosched.ui.schedule.agenda.ScheduleAgendaFragment
import com.google.samples.apps.iosched.ui.schedule.day.ScheduleDayFragment
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailActivity
import com.google.samples.apps.iosched.ui.setUpSnackbar
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment.Companion.DIALOG_NOTIFICATIONS_PREFERENCE
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignOutDialogFragment
import com.google.samples.apps.iosched.util.fabVisibility
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_COLLAPSED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_EXPANDED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_HIDDEN
import com.google.samples.apps.iosched.widget.FadingSnackbar
import dagger.android.support.DaggerFragment
import javax.inject.Inject

/**
 * The Schedule page of the top-level Activity.
 */
class ScheduleFragment : DaggerFragment(), MainNavigationFragment {

    companion object {
        private val COUNT = ConferenceDay.values().size + 1 // Agenda
        private val AGENDA_POSITION = COUNT - 1
        private const val DIALOG_NEED_TO_SIGN_IN = "dialog_need_to_sign_in"
        private const val DIALOG_CONFIRM_SIGN_OUT = "dialog_confirm_sign_out"
        private const val DIALOG_SCHEDULE_HINTS = "dialog_schedule_hints"
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var scheduleViewModel: ScheduleViewModel
    private lateinit var coordinatorLayout: CoordinatorLayout

    @Inject lateinit var snackbarMessageManager: SnackbarMessageManager

    private lateinit var filtersFab: FloatingActionButton
    private lateinit var viewPager: ViewPager
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var snackbar: FadingSnackbar

    // Stores the labels of the viewpager to avoid unnecessary recreation
    private var labelsForDays: List<Int>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        scheduleViewModel = activityViewModelProvider(viewModelFactory)
        val binding = FragmentScheduleBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@ScheduleFragment)
            viewModel = this@ScheduleFragment.scheduleViewModel
        }

        coordinatorLayout = binding.coordinatorLayout
        filtersFab = binding.filterFab
        snackbar = binding.snackbar
        viewPager = binding.viewpager
        // We can't lookup bottomSheetBehavior here since it's on a <fragment> tag

        val snackbarPrefViewModel: SnackbarPreferenceViewModel = viewModelProvider(viewModelFactory)
        setUpSnackbar(scheduleViewModel.snackBarMessage, snackbar, snackbarMessageManager,
            actionClickListener = {
                snackbarPrefViewModel.onStopClicked()
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
        scheduleViewModel.transientUiState.observe(this, Observer {
            updateFiltersUi(it ?: return@Observer)
        })

        if (savedInstanceState == null) {
            // VM outlives the UI, so reset this flag when a new Schedule page is shown
            scheduleViewModel.userHasInteracted = false
        }
        scheduleViewModel.currentEvent.observe(this, Observer { eventLocation ->
            if (eventLocation != null && !scheduleViewModel.userHasInteracted) {
                binding.viewpager.run {
                    post {
                        currentItem = eventLocation.day.ordinal
                    }
                }
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager.offscreenPageLimit = COUNT - 1

        val appbar: View = view.findViewById(R.id.appbar)
        val tabs: TabLayout = view.findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        viewPager.addOnPageChangeListener(object : SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                scheduleViewModel.setIsAgendaPage(position == AGENDA_POSITION)
            }
        })

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
                viewPager.importantForAccessibility = a11yState
                appbar.importantForAccessibility = a11yState
            }
        })

        scheduleViewModel.labelsForDays.observe(this, Observer<List<Int>> {
            it ?: return@Observer
            if (it != labelsForDays) { // Avoid unnecessary recreation.
                viewPager.adapter = ScheduleAdapter(childFragmentManager, it)
                labelsForDays = it
            }
        })

        if (savedInstanceState == null) {
            scheduleViewModel.setIsAgendaPage(false)
        }
    }

    private fun updateFiltersUi(uiState: TransientUiState) {
        val showFab = !uiState.isAgendaPage && !uiState.hasAnyFilters
        val hideable = uiState.isAgendaPage || !uiState.hasAnyFilters

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
        bottomSheetBehavior.isHideable = hideable
        bottomSheetBehavior.skipCollapsed = !uiState.hasAnyFilters
        if (hideable && bottomSheetBehavior.state == STATE_COLLAPSED) {
            bottomSheetBehavior.state = STATE_HIDDEN
        }
    }

    override fun onBackPressed(): Boolean {
        if (bottomSheetBehavior.state == STATE_EXPANDED) {
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

    /**
     * Adapter that builds a page for each conference day.
     */
    inner class ScheduleAdapter(fm: FragmentManager, private val labelsForDays: List<Int>) :
        FragmentPagerAdapter(fm) {

        override fun getCount() = COUNT

        override fun getItem(position: Int): Fragment {
            return when (position) {
                AGENDA_POSITION -> ScheduleAgendaFragment()
                else -> ScheduleDayFragment.newInstance(ConferenceDay.values()[position])
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when (position) {
                AGENDA_POSITION -> getString(R.string.agenda)
                else -> getString(labelsForDays[position])
            }
        }
    }
}

