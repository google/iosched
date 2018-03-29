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
import android.support.v4.app.FragmentActivity
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.OnHierarchyChangeListener
import androidx.view.doOnLayout
import com.google.android.material.widget.FloatingActionButton
import com.google.android.material.widget.Snackbar
import com.google.android.material.widget.Snackbar.LENGTH_LONG
import com.google.android.material.widget.Snackbar.LENGTH_SHORT
import com.google.android.material.widget.TabLayout
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleBinding
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestParameters
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.shared.util.lazyFast
import com.google.samples.apps.iosched.ui.MainActivity
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogFragment
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogFragment.Companion.DIALOG_REMOVE_RESERVATION
import com.google.samples.apps.iosched.ui.reservation.SwapReservationDialogFragment
import com.google.samples.apps.iosched.ui.reservation.SwapReservationDialogFragment.Companion.DIALOG_SWAP_RESERVATION
import com.google.samples.apps.iosched.ui.schedule.agenda.ScheduleAgendaFragment
import com.google.samples.apps.iosched.ui.schedule.day.ScheduleDayFragment
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailActivity
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.ui.signin.SignOutDialogFragment
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import com.google.samples.apps.iosched.widget.HideBottomViewOnScrollBehavior
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
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: ScheduleViewModel
    private lateinit var coordinatorLayout: CoordinatorLayout

    private lateinit var filtersFab: FloatingActionButton
    private lateinit var dummyBottomView: View
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    // Peek height we want to maintain above the bottom navigation
    private val basePeekHeight: Int by lazyFast {
        resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = activityViewModelProvider(viewModelFactory)
        val binding = FragmentScheduleBinding.inflate(inflater, container, false)
        coordinatorLayout = binding.coordinatorLayout

        // Set the layout variables
        binding.viewModel = viewModel
        binding.setLifecycleOwner(this)

        viewModel.navigateToSessionAction.observe(this, Observer { navigationEvent ->
            navigationEvent?.getContentIfNotHandled()?.let { sessionId ->
                openSessionDetail(sessionId)
            }
        })

        viewModel.navigateToSignInDialogAction.observe(this, Observer {
            it?.getContentIfNotHandled()?.let {
                openSignInDialog()
            }
        })

        viewModel.navigateToSignOutDialogAction.observe(this, Observer {
            it?.getContentIfNotHandled()?.let {
                openSignOutDialog()
            }
        })
        viewModel.navigateToRemoveReservationDialogAction.observe(this, Observer {
            it?.getContentIfNotHandled()?.let {
                openRemoveReservationDialog(requireActivity(), it)
            }
        })
        viewModel.navigateToSwapReservationDialogAction.observe(this, Observer {
            it?.getContentIfNotHandled()?.let {
                openSwapReservationDialog(requireActivity(), it)
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewpager: ViewPager = view.findViewById(R.id.viewpager)
        viewpager.offscreenPageLimit = COUNT - 1
        viewpager.adapter = ScheduleAdapter(childFragmentManager)

        val tabs: TabLayout = view.findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewpager)

        // Ensure snackbars appear above the hiding BottomNavigationView.
        // We clear the Snackbar's insetEdge, which is also set in it's (final) showView() method,
        // so we have to do it later (e.g. when it's added to the hierarchy).
        coordinatorLayout.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) {
                if (child is Snackbar.SnackbarLayout) {
                    child.layoutParams = (child.layoutParams as CoordinatorLayout.LayoutParams)
                        .apply {
                            insetEdge = Gravity.NO_GRAVITY
                            dodgeInsetEdges = Gravity.BOTTOM
                        }
                    // Also make it draw over the bottom sheet
                    child.elevation = resources.getDimension(R.dimen.bottom_sheet_elevation)
                }
            }

            override fun onChildViewRemoved(parent: View, child: View) {}
        })

        // Show snackbar messages generated by the ViewModel
        viewModel.snackBarMessage.observe(this, Observer {
            it?.getContentIfNotHandled()?.let { message: SnackbarMessage ->
                val duration = if (message.longDuration) LENGTH_LONG else LENGTH_SHORT
                Snackbar.make(coordinatorLayout, message.messageId, duration).apply {
                    message.actionId?.let { action ->
                        setAction(action, { this.dismiss() })
                    }
                    setActionTextColor(ContextCompat.getColor(context, R.color.teal))
                    show()
                }
            }
        })

        filtersFab = view.findViewById(R.id.filter_fab)
        filtersFab.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        dummyBottomView = view.findViewById(R.id.dummy_bottom_navigation)
        bottomSheetBehavior = BottomSheetBehavior.from(view.findViewById(R.id.filter_sheet))
        bottomSheetBehavior.skipCollapsed = true

        // Lock the bottom navigation hidden while the filters sheet is expanded.
        bottomSheetBehavior.setBottomSheetCallback(object : BottomSheetCallback {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val activity = requireActivity() as MainActivity
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED ->
                        activity.setBottomNavLockMode(
                            HideBottomViewOnScrollBehavior.LOCK_MODE_LOCKED_HIDDEN)

                    BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_HIDDEN ->
                        activity.setBottomNavLockMode(
                            HideBottomViewOnScrollBehavior.LOCK_MODE_UNLOCKED)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        // We can't use DataBinding on <fragment> tags, so set up an observer manually.
        viewModel.hasAnyFilters.observe(this, Observer {
            val hasFilters = it ?: false
            bottomSheetBehavior.isHideable = !hasFilters
            bottomSheetBehavior.skipCollapsed = !hasFilters
            if (!hasFilters && bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        })

        if (savedInstanceState == null) {
            // Set the peek height on first layout
            dummyBottomView.doOnLayout { onBottomNavSlide(dummyBottomView.translationY) }
            // Make bottom sheet hidden at first
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    override fun onBottomNavSlide(bottonNavTranslationY: Float) {
        // Move the dummy view to change bottom edge inset (for snackbars, etc.)
        dummyBottomView.translationY = bottonNavTranslationY
        // Tie the filters bottom sheet to the bottom navigation bar
        val peek = Math.max(0, (dummyBottomView.height - bottonNavTranslationY + .5f).toInt())
        bottomSheetBehavior.peekHeight = basePeekHeight + peek
    }

    private fun openSessionDetail(id: String) {
        startActivity(SessionDetailActivity.starterIntent(requireContext(), id))
    }

    private fun openSignInDialog() {
        val dialog = SignInDialogFragment()
        dialog.show(requireActivity().supportFragmentManager, DIALOG_NEED_TO_SIGN_IN)
    }

    private fun openRemoveReservationDialog(
        activity: FragmentActivity,
        parameters: ReservationRequestParameters
    ) {
        val dialog = RemoveReservationDialogFragment.newInstance(parameters)
        dialog.show(activity.supportFragmentManager, DIALOG_REMOVE_RESERVATION)
    }

    private fun openSwapReservationDialog(
            activity: FragmentActivity,
            parameters: SwapRequestParameters
    ) {
        val dialog = SwapReservationDialogFragment.newInstance(parameters)
        dialog.show(activity.supportFragmentManager, DIALOG_SWAP_RESERVATION)
    }

    private fun openSignOutDialog() {
        val dialog = SignOutDialogFragment()
        dialog.show(requireActivity().supportFragmentManager, DIALOG_CONFIRM_SIGN_OUT)
    }

    /**
     * Adapter that build a page for each conference day.
     */
    inner class ScheduleAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

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
                else -> ConferenceDay.values()[position].formatMonthDay()
            }
        }
    }
}
