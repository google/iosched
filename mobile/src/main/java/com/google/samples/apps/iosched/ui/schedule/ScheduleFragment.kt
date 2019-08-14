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
import androidx.activity.OnBackPressedCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleBinding
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.dialogs.NotificationsPreferencesDialogDispatcher
import com.google.samples.apps.iosched.ui.dialogs.SignInDialogDispatcher
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.prefs.SnackbarPreferenceViewModel
import com.google.samples.apps.iosched.ui.schedule.day.ScheduleDayFragment
import com.google.samples.apps.iosched.ui.setUpSnackbar
import com.google.samples.apps.iosched.util.fabVisibility
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_COLLAPSED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_DRAGGING
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_EXPANDED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_HALF_EXPANDED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_HIDDEN
import com.google.samples.apps.iosched.widget.FadingSnackbar
import dagger.android.support.DaggerFragment
import javax.inject.Inject

/**
 * The Schedule page of the top-level Activity.
 */
class ScheduleFragment : DaggerFragment(), MainNavigationFragment {

    companion object {
        private val COUNT = ConferenceDays.size
    }

    @Inject lateinit var analyticsHelper: AnalyticsHelper

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var scheduleViewModel: ScheduleViewModel
    private lateinit var coordinatorLayout: CoordinatorLayout

    @Inject lateinit var snackbarMessageManager: SnackbarMessageManager

    private lateinit var filtersFab: FloatingActionButton
    private lateinit var viewPager: ViewPager
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var snackbar: FadingSnackbar

    @Inject lateinit var signInDialogDispatcher: SignInDialogDispatcher
    @Inject lateinit var notificationsDialogDispatcher: NotificationsPreferencesDialogDispatcher

    // Stores the labels of the viewpager to avoid unnecessary recreation
    private var labelsForDays: List<Int>? = null

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

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

        scheduleViewModel.navigateToSearchAction.observe(this, EventObserver {
            openSearch()
        })

        scheduleViewModel.navigateToSignOutDialogAction.observe(this, EventObserver {
            signInDialogDispatcher.openSignOutDialog(requireActivity())
        })
        scheduleViewModel.shouldShowNotificationsPrefAction.observe(this, EventObserver {
            if (it) {
                notificationsDialogDispatcher.startDialog(requireActivity())
            }
        })
        scheduleViewModel.hasAnyFilters.observe(this, Observer {
            updateFiltersUi(it ?: return@Observer)
        })

        if (savedInstanceState == null) {
            // VM outlives the UI, so reset this flag when a new Schedule page is shown
            scheduleViewModel.userHasInteracted = false
        }
        scheduleViewModel.currentEvent.observe(this, Observer { eventLocation ->
            if (!scheduleViewModel.userHasInteracted) {
                if (eventLocation != null) {
                    // switch to the current day
                    binding.viewpager.run {
                        post {
                            // this will trigger onPageChanged and log the page view
                            currentItem = eventLocation.day
                        }
                    }
                } else {
                    // Showing the default page. Log it.
                    logAnalyticsPageView(binding.viewpager.currentItem)
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
                logAnalyticsPageView(position)
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
                onBackPressedCallback.isEnabled = when (newState) {
                    STATE_DRAGGING, STATE_EXPANDED, STATE_HALF_EXPANDED -> true
                    else -> false
                }
            }
        })

        scheduleViewModel.labelsForDays.observe(this, Observer<List<Int>> {
            it ?: return@Observer
            if (it != labelsForDays || viewPager.adapter == null) { // Avoid unnecessary recreation.
                // Note that recreating the adapter doesn't mean fragments are recreated
                viewPager.adapter = ScheduleAdapter(childFragmentManager, it)
                labelsForDays = it
            }
        })
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

    private fun onBackPressed() {
        if (::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state == STATE_EXPANDED) {
            // collapse or hide the sheet
            if (bottomSheetBehavior.isHideable && bottomSheetBehavior.skipCollapsed) {
                bottomSheetBehavior.state = STATE_HIDDEN
            } else {
                bottomSheetBehavior.state = STATE_COLLAPSED
            }
        }
    }

    override fun onUserInteraction() {
        // Guard against a crash.
        // Rarely observed the method was called before the ViewModel was initialized.
        if (::scheduleViewModel.isInitialized) {
            scheduleViewModel.userHasInteracted = true
        }
    }

    private fun openSessionDetail(id: SessionId) {
        val action = ScheduleFragmentDirections.actionScheduleFragmentToSessionDetailFragment(id)
        findNavController().navigate(action)
    }

    private fun openSearch() {
        val action = ScheduleFragmentDirections.actionScheduleFragmentToSearchFragment()
        findNavController().navigate(action)
    }

    private fun openSignInDialog() {
        signInDialogDispatcher.openSignInDialog(requireActivity())
    }

    private fun logAnalyticsPageView(position: Int) {
        analyticsHelper.sendScreenView("Schedule - Day ${position + 1}", requireActivity())
    }

    /**
     * Adapter that builds a page for each conference day.
     */
    inner class ScheduleAdapter(fm: FragmentManager, private val labelsForDays: List<Int>) :
        FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getCount() = COUNT

        override fun getItem(position: Int): Fragment = ScheduleDayFragment.newInstance(position)

        override fun getPageTitle(position: Int): CharSequence = getString(labelsForDays[position])
    }
}
