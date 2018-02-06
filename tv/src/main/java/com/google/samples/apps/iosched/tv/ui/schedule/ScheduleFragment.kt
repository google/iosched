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

package com.google.samples.apps.iosched.tv.ui.schedule

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v17.leanback.app.RowsSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.support.v17.leanback.widget.Presenter
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.samples.apps.iosched.shared.util.inTransaction
import com.google.samples.apps.iosched.tv.R
import com.google.samples.apps.iosched.tv.ui.SpinnerFragment

/**
 * Displays a single day's session schedule.
 */
class ScheduleFragment : RowsSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    private val spinnerFragment = SpinnerFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = rowsAdapter

        fragmentManager?.inTransaction {
            add(R.id.main_frame, spinnerFragment)
        }

        // TODO: Inject view model factory
        val viewModel: ScheduleViewModel = ViewModelProviders.of(
                this, TODO("ViewModel Factory not provided."))
                .get(ScheduleViewModel::class.java)

        observeViewModel(viewModel)
    }

    override fun onStop() {
        super.onStop()
        fragmentManager?.inTransaction {
            remove(spinnerFragment)
        }
    }

    private fun loadAdapter() {
        // TODO: replace with real data.
        val dummyAdapter = ArrayObjectAdapter(object : Presenter() {
            override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
                val view = LayoutInflater.from(parent?.context)
                        .inflate(R.layout.card_session, parent, false)
                return ViewHolder(view)
            }

            override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {}

            override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}

        }).apply {
            add("Goodbye World")
        }

        val dummyheader = HeaderItem(1, "Dummy Header")
        val dummyRow = ListRow(dummyheader, dummyAdapter)

        rowsAdapter.add(dummyRow)

        mainFragmentAdapter.fragmentHost.notifyDataReady(mainFragmentAdapter)
    }

    private fun observeViewModel(viewModel: ScheduleViewModel) {

        // Update text if there are sessions available
        viewModel.sessions.observe(this, Observer { sessions ->
            loadAdapter()
        })

        // Update text if the screen is in loading state.
        viewModel.isLoading.observe(this, Observer { isLoading ->

            if (isLoading == false) {
                fragmentManager?.inTransaction {
                    remove(spinnerFragment)
                }
            }
        })
    }
}
