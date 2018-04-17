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

package com.google.samples.apps.iosched.ui.feed

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.databinding.BindingAdapter
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentFeedBinding
import com.google.samples.apps.iosched.shared.model.FeedItem
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.setUpSnackbar
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.toolbar.*
import timber.log.Timber
import javax.inject.Inject

class FeedFragment : DaggerFragment(), MainNavigationFragment {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    private lateinit var model: FeedViewModel

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?
    ): View? {
        model = viewModelProvider(viewModelFactory)

        val binding = FragmentFeedBinding.inflate(
                inflater, container, false).apply {
            setLifecycleOwner(this@FeedFragment)
            viewModel = model
        }

        setUpSnackbar(model.snackBarMessage, binding.snackbar, snackbarMessageManager)

        model.errorMessage.observe(this, Observer { message ->
            val errorMessage = message?.getContentIfNotHandled()
            if (!errorMessage.isNullOrEmpty()) {
                Timber.e(errorMessage)
            }
        })

        model.loadFeed()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setTitle(R.string.title_feed)
    }
}

@BindingAdapter("feedItems")
fun feedItems(recyclerView: RecyclerView, list: List<FeedItem>?) {
    if (recyclerView.adapter == null) {
        recyclerView.adapter = FeedAdapter()
    }
    (recyclerView.adapter as FeedAdapter).submitList(list ?: emptyList())
}