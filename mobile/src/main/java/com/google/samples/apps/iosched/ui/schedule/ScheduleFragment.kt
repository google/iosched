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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleBinding
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.di.SearchScheduleEnabledFlag
import com.google.samples.apps.iosched.shared.domain.sessions.ConferenceDayIndexer
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.ui.MainActivityViewModel
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.ScheduleNavigationAction.NavigateToSignInDialogAction
import com.google.samples.apps.iosched.ui.schedule.ScheduleNavigationAction.NavigateToSignOutDialogAction
import com.google.samples.apps.iosched.ui.schedule.ScheduleNavigationAction.ShowScheduleUiHints
import com.google.samples.apps.iosched.ui.sessioncommon.SessionsAdapter
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment.Companion.DIALOG_NOTIFICATIONS_PREFERENCE
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignInNavigationAction.ShowNotificationPreferencesDialog
import com.google.samples.apps.iosched.ui.signin.SignOutDialogFragment
import com.google.samples.apps.iosched.ui.signin.setupProfileMenuItem
import com.google.samples.apps.iosched.util.clearDecorations
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.executeAfter
import com.google.samples.apps.iosched.util.launchAndRepeatWithViewLifecycle
import com.google.samples.apps.iosched.util.requestApplyInsetsWhenAttached
import com.google.samples.apps.iosched.widget.BubbleDecoration
import com.google.samples.apps.iosched.widget.FadingSnackbar
import com.google.samples.apps.iosched.widget.JumpSmoothScroller
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * The Schedule page of the top-level Activity.
 */
@AndroidEntryPoint
class ScheduleFragment : Fragment() {

    companion object {
        private const val DIALOG_NEED_TO_SIGN_IN = "dialog_need_to_sign_in"
        private const val DIALOG_CONFIRM_SIGN_OUT = "dialog_confirm_sign_out"
        private const val DIALOG_SCHEDULE_HINTS = "dialog_schedule_hints"
    }

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    @field:Named("tagViewPool")
    lateinit var tagViewPool: RecycledViewPool

    @Inject
    @JvmField
    @SearchScheduleEnabledFlag
    var searchScheduleFeatureEnabled: Boolean = false

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    private val scheduleViewModel: ScheduleViewModel by viewModels()
    private val scheduleTwoPaneViewModel: ScheduleTwoPaneViewModel by activityViewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    private lateinit var snackbar: FadingSnackbar

    private lateinit var scheduleRecyclerView: RecyclerView
    private lateinit var sessionsAdapter: SessionsAdapter
    private lateinit var scheduleScroller: JumpSmoothScroller

    private lateinit var dayIndicatorRecyclerView: RecyclerView
    private lateinit var dayIndicatorAdapter: DayIndicatorAdapter
    private lateinit var dayIndicatorItemDecoration: BubbleDecoration

    private lateinit var dayIndexer: ConferenceDayIndexer
    private var cachedBubbleRange: IntRange? = null

    private lateinit var binding: FragmentScheduleBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScheduleBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = scheduleViewModel
        }

        snackbar = binding.snackbar
        scheduleRecyclerView = binding.recyclerviewSchedule
        dayIndicatorRecyclerView = binding.dayIndicators
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up search menu item
        binding.toolbar.run {
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

        binding.toolbar.setupProfileMenuItem(mainActivityViewModel, this)

        // Pad the bottom of the RecyclerView so that the content scrolls up above the nav bar
        binding.recyclerviewSchedule.doOnApplyWindowInsets { v, insets, padding ->
            val systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(bottom = padding.bottom + systemInsets.bottom)
        }

        // Session list configuration
        sessionsAdapter = SessionsAdapter(
            tagViewPool,
            scheduleViewModel.showReservations,
            scheduleViewModel.timeZoneId,
            viewLifecycleOwner,
            scheduleTwoPaneViewModel, // OnSessionClickListener
            scheduleTwoPaneViewModel // OnSessionStarClickListener
        )
        scheduleRecyclerView.apply {
            adapter = sessionsAdapter
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

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    scheduleViewModel.userHasInteracted = true
                }
            })
        }
        scheduleScroller = JumpSmoothScroller(view.context)

        dayIndicatorItemDecoration = BubbleDecoration(view.context)
        dayIndicatorRecyclerView.addItemDecoration(dayIndicatorItemDecoration)

        dayIndicatorAdapter = DayIndicatorAdapter(scheduleViewModel, viewLifecycleOwner)
        dayIndicatorRecyclerView.adapter = dayIndicatorAdapter

        // Start observing ViewModels
        launchAndRepeatWithViewLifecycle {
            launch {
                scheduleViewModel.scheduleUiData.collect { updateScheduleUi(it) }
            }

            // During conference, scroll to current event.
            launch {
                scheduleViewModel.scrollToEvent.collect { scrollEvent ->
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
                }
            }

            launch {
                scheduleViewModel.navigationActions.collect {
                    when (it) {
                        is NavigateToSignInDialogAction -> openSignInDialog()
                        is NavigateToSignOutDialogAction -> openSignOutDialog()
                        is ShowScheduleUiHints -> openScheduleUiHintsDialog()
                    }
                }
            }

            launch {
                scheduleViewModel.signInNavigationActions.collect {
                    if (it == ShowNotificationPreferencesDialog) {
                        openNotificationsPreferenceDialog()
                    }
                }
            }

            // Show an error message
            launch {
                scheduleViewModel.errorMessage.collect { errorMsg ->
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }

        if (savedInstanceState == null) {
            // VM outlives the UI, so reset this flag when a new Schedule page is shown
            scheduleViewModel.userHasInteracted = false

            binding.coordinatorLayout.postDelayed(
                {
                    binding.coordinatorLayout.requestApplyInsetsWhenAttached()
                },
                500
            )
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

        sessionsAdapter.submitList(list)
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

    private fun openSearch() {
        findNavController().navigate(ScheduleFragmentDirections.toSearch())
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
