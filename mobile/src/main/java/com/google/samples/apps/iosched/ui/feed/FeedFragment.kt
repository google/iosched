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
import androidx.core.view.updatePadding
import androidx.databinding.BindingAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.common.collect.ImmutableMap
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentFeedBinding
import com.google.samples.apps.iosched.model.feed.Announcement
import com.google.samples.apps.iosched.model.feed.FeedItem
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.setUpSnackbar
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
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
            v.updatePadding(bottom = padding.bottom + insets.systemWindowInsetBottom)
        }

        setUpSnackbar(model.snackBarMessage, binding.snackbar, snackbarMessageManager)

        model.errorMessage.observe(this, Observer { message ->
            val errorMessage = message?.getContentIfNotHandled()
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this.context, errorMessage, Toast.LENGTH_SHORT).show()
                Timber.e(errorMessage)
            }
        })

        model.loadFeed()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setTitle(R.string.title_feed)
    }
}

@BindingAdapter("feedItems")
fun feedItems(recyclerView: RecyclerView, list: List<FeedItem>?) {
    if (recyclerView.adapter == null) {
        val viewBinders: ImmutableMap<FeedItemClass, FeedBinder>

        val feedAnnouncementViewBinder =
            FeedAnnouncementViewBinder(Announcement::class.java, recyclerView.context)
        val feedAnnouncementHeaderViewBinder =
            FeedAnnouncementHeaderViewBinder(SectionHeader::class.java, recyclerView.context)
        val countdownTimerViewBinder =
            CountdownTimerViewBinder(CountdownTimer::class.java, recyclerView.context)

        viewBinders =
            ImmutableMap.builder<FeedItemClass, FeedBinder>()
                .put(
                    feedAnnouncementViewBinder.modelClass,
                    feedAnnouncementViewBinder as FeedBinder
                )
                .put(
                    feedAnnouncementHeaderViewBinder.modelClass,
                    feedAnnouncementHeaderViewBinder as FeedBinder
                )
                .put(
                    countdownTimerViewBinder.modelClass,
                    countdownTimerViewBinder as FeedBinder
                )
                .build()

        recyclerView.adapter =
            FeedAdapter(FeedDiffCallback(viewBinders = viewBinders), viewBinders = viewBinders)
    }
    (recyclerView.adapter as FeedAdapter).submitList(list ?: emptyList())
}