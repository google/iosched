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
import androidx.activity.addCallback
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleBinding
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.di.SearchScheduleEnabledFlag
import com.google.samples.apps.iosched.shared.domain.sessions.ConferenceDayIndexer
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.prefs.SnackbarPreferenceViewModel
import com.google.samples.apps.iosched.ui.schedule.ScheduleFragmentDirections.Companion.toSearch
import com.google.samples.apps.iosched.ui.schedule.ScheduleFragmentDirections.Companion.toSessionDetail
import com.google.samples.apps.iosched.ui.setUpSnackbar
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment.Companion.DIALOG_NOTIFICATIONS_PREFERENCE
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignOutDialogFragment
import com.google.samples.apps.iosched.ui.signin.setupProfileMenuItem
import com.google.samples.apps.iosched.util.clearDecorations
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.executeAfter
import com.google.samples.apps.iosched.util.fabVisibility
import com.google.samples.apps.iosched.util.requestApplyInsetsWhenAttached
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_COLLAPSED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_EXPANDED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_HIDDEN
import com.google.samples.apps.iosched.widget.BubbleDecoration
import com.google.samples.apps.iosched.widget.FadingSnackbar
import com.google.samples.apps.iosched.widget.JumpSmoothScroller
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

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    @field:Named("tagViewPool")
    lateinit var tagViewPool: RecycledViewPool

    @Inject
    @JvmField
    @SearchScheduleEnabledFlag
    var searchScheduleFeatureEnabled: Boolean = false

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    private lateinit var scheduleViewModel: ScheduleViewModel

    private lateinit var filtersFab: FloatingActionButton
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var snackbar: FadingSnackbar

    private lateinit var scheduleRecyclerView: RecyclerView
    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var scheduleScroller: JumpSmoothScroller

    private lateinit var dayIndicatorRecyclerView: RecyclerView
    private lateinit var dayIndicatorAdapter: DayIndicatorAdapter
    private lateinit var dayIndicatorItemDecoration: BubbleDecoration

    private lateinit var dayIndexer: ConferenceDayIndexer
    private var cachedBubbleRange: IntRange? = null

    private lateinit var binding: FragmentScheduleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            onBackPressed()
        }
    }

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
        scheduleRecyclerView = binding.recyclerviewSchedule
        dayIndicatorRecyclerView = binding.includeScheduleAppbar.dayIndicators
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up search menu item
        binding.includeScheduleAppbar.toolbar.run {
            inflateMenu(R.menu.schedule_menu)
            menu.findItem(R.id.search).isVisible = searchScheduleFeatureEnabled
            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.search) {
                    analyticsHelper.logUiEvent("Navigate to Search", AnalyticsActions.CLICK)
                    openSearch()
                    true
                } else {
                    false
                }
            }
        }

        // Snackbar configuration
        val snackbarPrefViewModel: SnackbarPreferenceViewModel = viewModelProvider(viewModelFactory)
        setUpSnackbar(scheduleViewModel.snackBarMessage, snackbar, snackbarMessageManager,
            actionClickListener = {
                snackbarPrefViewModel.onStopClicked()
            }
        )

        binding.includeScheduleAppbar.toolbar.setupProfileMenuItem(
            activityViewModelProvider(viewModelFactory), this
        )

        // Filters sheet configuration
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
                scheduleRecyclerView.importantForAccessibility = a11yState
                binding.includeScheduleAppbar.appbar.importantForAccessibility = a11yState
            }
        })

        // Pad the bottom of the RecyclerView so that the content scrolls up above the nav bar
        binding.recyclerviewSchedule.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePaddingRelative(bottom = padding.bottom + insets.systemWindowInsetBottom)
        }

        // Session list configuration
        scheduleAdapter = ScheduleAdapter(
            scheduleViewModel,
            tagViewPool,
            scheduleViewModel.showReservations,
            scheduleViewModel.timeZoneId,
            this
        )
        scheduleRecyclerView.apply {
            adapter = scheduleAdapter
            (itemAnimator as DefaultItemAnimator).run {
                supportsChangeAnimations = false
                addDuration = 160L
                moveDuration = 160L
                changeDuration = 160L
                removeDuration = 120L
            }

            addOnScrollListener(object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    onScheduleScrolled()
                }
            })
        }
        scheduleScroller = JumpSmoothScroller(view.context)

        dayIndicatorItemDecoration = BubbleDecoration(view.context)
        dayIndicatorRecyclerView.addItemDecoration(dayIndicatorItemDecoration)

        dayIndicatorAdapter = DayIndicatorAdapter(scheduleViewModel, viewLifecycleOwner)
        dayIndicatorRecyclerView.adapter = dayIndicatorAdapter

        // Start observing ViewModels
        scheduleViewModel.scheduleUiData.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            updateScheduleUi(it)
        })

        // During conference, scroll to current event.
        scheduleViewModel.scrollToEvent.observe(viewLifecycleOwner, EventObserver { scrollEvent ->
            if (scrollEvent.targetPosition != -1) {
                scheduleRecyclerView.run {
                    post {
                        val lm = layoutManager as LinearLayoutManager
                        if (scrollEvent.smoothScroll) {
                            scheduleScroller.targetPosition = scrollEvent.targetPosition
                            lm.startSmoothScroll(scheduleScroller)
                        } else {
                            lm.scrollToPositionWithOffset(scrollEvent.targetPosition, 0)
                        }
                    }
                }
            }
        })

        scheduleViewModel.navigateToSessionAction.observe(
            viewLifecycleOwner,
            EventObserver { sessionId ->
                openSessionDetail(sessionId)
            }
        )

        scheduleViewModel.navigateToSignInDialogAction.observe(viewLifecycleOwner, EventObserver {
            openSignInDialog()
        })

        scheduleViewModel.navigateToSignOutDialogAction.observe(viewLifecycleOwner, EventObserver {
            openSignOutDialog()
        })
        scheduleViewModel.scheduleUiHintsShown.observe(viewLifecycleOwner, EventObserver {
            if (!it) {
                openScheduleUiHintsDialog()
            }
        })
        scheduleViewModel.shouldShowNotificationsPrefAction.observe(
            viewLifecycleOwner,
            EventObserver {
                if (it) {
                    openNotificationsPreferenceDialog()
                }
            }
        )
        scheduleViewModel.hasAnyFilters.observe(viewLifecycleOwner, Observer {
            updateFiltersUi(it ?: return@Observer)
        })

        // Show an error message
        scheduleViewModel.errorMessage.observe(viewLifecycleOwner, EventObserver { errorMsg ->
            // TODO: Change once there's a way to show errors to the user
            Toast.makeText(this.context, errorMsg, Toast.LENGTH_LONG).show()
        })

        if (savedInstanceState == null) {
            // VM outlives the UI, so reset this flag when a new Schedule page is shown
            scheduleViewModel.userHasInteracted = false

            binding.coordinatorLayout.postDelayed({
                binding.coordinatorLayout.requestApplyInsetsWhenAttached()
            }, 500)

            // Process arguments to set initial filters
            arguments?.let {
                if (ScheduleFragmentArgs.fromBundle(it).showPinnedEvents) {
                    scheduleViewModel.clearFilters()
                    scheduleViewModel.showPinnedEvents()
                }
                if (ScheduleFragmentArgs.fromBundle(it).showAllEvents) {
                    scheduleViewModel.clearFilters()
                }
            }
        }
        analyticsHelper.sendScreenView("Schedule", requireActivity())
    }

    private fun updateScheduleUi(scheduleUiData: ScheduleUiData) {
        // Require everything to be loaded.
        val list = scheduleUiData.list ?: return
        val timeZoneId = scheduleUiData.timeZoneId ?: return
        val indexer = scheduleUiData.dayIndexer ?: return

        dayIndexer = indexer
        // Prevent building new indicators until we get scroll information.
        cachedBubbleRange = null
        if (indexer.days.isEmpty()) {
            // Special case: the results are empty, so we won't get valid scroll information.
            // Set a bogus range to and rebuild the day indicators.
            cachedBubbleRange = -1..-1
            rebuildDayIndicators()
        }

        scheduleAdapter.submitList(list)
        scheduleRecyclerView.run {
            // Recreate the decoration used for the sticky time headers
            clearDecorations()
            if (list.isNotEmpty()) {
                addItemDecoration(
                    ScheduleTimeHeadersDecoration(
                        context, list.map { it.session }, timeZoneId
                    )
                )
                addItemDecoration(
                    DaySeparatorItemDecoration(
                        context, indexer, timeZoneId
                    )
                )
            }
        }

        binding.executeAfter {
            isEmpty = list.isEmpty()
        }
    }

    private fun rebuildDayIndicators() {
        // cachedBubbleRange will get set once we have scroll information, so wait until then.
        val bubbleRange = cachedBubbleRange ?: return
        val indicators = if (dayIndexer.days.isEmpty()) {
            TimeUtils.ConferenceDays.map { day: ConferenceDay ->
                DayIndicator(day = day, enabled = false)
            }
        } else {
            dayIndexer.days.mapIndexed { index: Int, day: ConferenceDay ->
                DayIndicator(day = day, checked = index in bubbleRange)
            }
        }

        dayIndicatorAdapter.submitList(indicators)
        dayIndicatorItemDecoration.bubbleRange = bubbleRange
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
                    R.dimen.schedule_bottom_sheet_peek_height
                }
            )
        }
        bottomSheetBehavior.isHideable = showFab
        bottomSheetBehavior.skipCollapsed = showFab
        if (showFab && bottomSheetBehavior.state == STATE_COLLAPSED) {
            bottomSheetBehavior.state = STATE_HIDDEN
        }
    }

    private fun onScheduleScrolled() {
        val layoutManager = (scheduleRecyclerView.layoutManager) as LinearLayoutManager
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()
        if (first < 0 || last < 0) {
            // When the list is empty, we get -1 for the positions.
            return
        }

        val firstDay = dayIndexer.dayForPosition(first) ?: return
        val lastDay = dayIndexer.dayForPosition(last) ?: return
        val highlightRange = dayIndexer.days.indexOf(firstDay)..dayIndexer.days.indexOf(lastDay)
        if (highlightRange != cachedBubbleRange) {
            cachedBubbleRange = highlightRange
            rebuildDayIndicators()
        }
    }

    private fun onBackPressed(): Boolean {
        if (::bottomSheetBehavior.isInitialized && bottomSheetBehavior.state == STATE_EXPANDED) {
            // collapse or hide the sheet
            if (bottomSheetBehavior.isHideable && bottomSheetBehavior.skipCollapsed) {
                bottomSheetBehavior.state = STATE_HIDDEN
            } else {
                bottomSheetBehavior.state = STATE_COLLAPSED
            }
            return true
        }
        return false
    }

    override fun onUserInteraction() {
        // Guard against a crash.
        // Rarely observed the method was called before the ViewModel was initialized.
        if (::scheduleViewModel.isInitialized) {
            scheduleViewModel.userHasInteracted = true
        }
    }

    private fun openSessionDetail(id: SessionId) {
        findNavController().navigate(toSessionDetail(id))
    }

    private fun openSearch() {
        findNavController().navigate(toSearch())
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
}
