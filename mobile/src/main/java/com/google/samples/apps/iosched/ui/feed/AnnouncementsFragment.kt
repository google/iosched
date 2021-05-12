/*
 * Copyright 2020 Google LLC
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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.common.collect.ImmutableMap
import com.google.samples.apps.iosched.databinding.FragmentAnnouncementsBinding
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.ui.MainActivityViewModel
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.signin.setupProfileMenuItem
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AnnouncementsFragment : MainNavigationFragment() {

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    private val model: AnnouncementsViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    private lateinit var binding: FragmentAnnouncementsBinding
    private lateinit var adapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAnnouncementsBinding.inflate(inflater, container, false)
            .apply {
                lifecycleOwner = viewLifecycleOwner
                viewModel = model
            }
        adapter = createAdapter()
        binding.recyclerView.adapter = adapter
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        analyticsHelper.sendScreenView("Announcements", requireActivity())

        binding.toolbar.setupProfileMenuItem(mainActivityViewModel, viewLifecycleOwner)

        binding.root.doOnApplyWindowInsets { _, insets, _ ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.statusBar.run {
                layoutParams.height = systemInsets.top
                isVisible = layoutParams.height > 0
                requestLayout()
            }
        }

        binding.recyclerView.doOnApplyWindowInsets { v, insets, padding ->
            val systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(bottom = padding.bottom + systemInsets.bottom)
        }

        model.announcements.observe(
            viewLifecycleOwner,
            Observer {
                (binding.recyclerView.adapter as FeedAdapter).submitList(it)
            }
        )
    }

    private fun createAdapter(): FeedAdapter {
        val announcementViewBinder = AnnouncementViewBinder(model.timeZoneId, this)
        val announcementsEmptyViewBinder = AnnouncementsEmptyViewBinder()
        val announcementsLoadingViewBinder = AnnouncementsLoadingViewBinder()

        @Suppress("UNCHECKED_CAST")
        return FeedAdapter(
            ImmutableMap.builder<FeedItemClass, FeedItemBinder>()
                .put(
                    announcementViewBinder.modelClass,
                    announcementViewBinder as FeedItemBinder
                )
                .put(
                    announcementsEmptyViewBinder.modelClass,
                    announcementsEmptyViewBinder as FeedItemBinder
                )
                .put(
                    announcementsLoadingViewBinder.modelClass,
                    announcementsLoadingViewBinder as FeedItemBinder
                ).build()
        )
    }
}
