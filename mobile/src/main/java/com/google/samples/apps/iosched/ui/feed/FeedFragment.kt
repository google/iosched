/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.google.common.collect.ImmutableMap
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentFeedBinding
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.ScheduleFragmentArgs
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailActivity
import com.google.samples.apps.iosched.ui.setUpSnackbar
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.getTappableElementInsetsAsRect
import kotlinx.android.synthetic.main.fragment_feed.toolbar
import timber.log.Timber
import javax.inject.Inject

class FeedFragment : MainNavigationFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    private lateinit var model: FeedViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        model = viewModelProvider(viewModelFactory)

        val binding = FragmentFeedBinding.inflate(
            inflater, container, false
        ).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = model
        }

        binding.root.doOnApplyWindowInsets { _, insets, _ ->
            binding.statusBar.run {
                layoutParams.height = insets.systemWindowInsetTop
                isVisible = layoutParams.height > 0
                requestLayout()
            }
        }

        binding.recyclerView.doOnApplyWindowInsets { v, insets, padding ->
            val tappableInsets = insets.getTappableElementInsetsAsRect()
            v.updatePaddingRelative(bottom = padding.bottom + tappableInsets.bottom)
        }

        setUpSnackbar(model.snackBarMessage, binding.snackbar, snackbarMessageManager)

        model.errorMessage.observe(this, Observer { message ->
            val errorMessage = message?.getContentIfNotHandled()
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this.context, errorMessage, Toast.LENGTH_SHORT).show()
                Timber.e(errorMessage)
            }
        })

        model.feed.observe(this, Observer {
            showFeedItems(binding.recyclerView, it)
        })

        model.navigateToSessionAction.observe(this, EventObserver { sessionId ->
            openSessionDetail(sessionId)
        })

        model.navigateToScheduleAction.observe(this, EventObserver { withPinnedEvents ->
            openSchedule(withPinnedEvents)
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.title_feed)
    }

    private fun openSessionDetail(id: SessionId) {
        startActivity(SessionDetailActivity.starterIntent(requireContext(), id))
    }

    private fun openSchedule(withPinnedSessions: Boolean) {
        if (withPinnedSessions) {
            NavHostFragment.findNavController(this).navigate(
                R.id.navigation_schedule,
                ScheduleFragmentArgs(showPinnedEvents = true).toBundle()
            )
        } else {
            NavHostFragment.findNavController(this).navigate(
                R.id.navigation_schedule,
                ScheduleFragmentArgs(showAllEvents = true).toBundle()
            )
        }
    }

    private fun showFeedItems(recyclerView: RecyclerView, list: List<Any>?) {
        if (recyclerView.adapter == null) {
            val announcementViewBinder = FeedAnnouncementViewBinder()
            val sectionHeaderViewBinder = FeedSectionHeaderViewBinder()
            val countdownTimerViewBinder = CountdownTimerViewBinder()
            val sessionsViewBinder = FeedSessionsViewBinder(model)
            val viewBinders = ImmutableMap.builder<FeedItemClass, FeedItemBinder>()
                .put(
                    announcementViewBinder.modelClass,
                    announcementViewBinder as FeedItemBinder
                )
                .put(
                    sectionHeaderViewBinder.modelClass,
                    sectionHeaderViewBinder as FeedItemBinder
                )
                .put(
                    countdownTimerViewBinder.modelClass,
                    countdownTimerViewBinder as FeedItemBinder
                )
                .put(
                    sessionsViewBinder.modelClass,
                    sessionsViewBinder as FeedItemBinder
                )
                .build()

            recyclerView.adapter = FeedAdapter(viewBinders)
        }
        (recyclerView.adapter as FeedAdapter).submitList(list ?: emptyList())
    }
}
