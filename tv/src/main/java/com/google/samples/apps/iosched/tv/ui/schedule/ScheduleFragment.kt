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
import android.os.Bundle
import android.support.v17.leanback.app.RowsSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.DiffCallback
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ImageCardView
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.support.v17.leanback.widget.Presenter
import android.text.TextUtils
import android.view.ViewGroup
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.inTransaction
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.tv.R
import com.google.samples.apps.iosched.tv.TvApplication
import com.google.samples.apps.iosched.tv.ui.SpinnerFragment
import com.google.samples.apps.iosched.tv.ui.presenter.SessionPresenter
import com.google.samples.apps.iosched.tv.util.toArrayObjectAdapter
import javax.inject.Inject

/**
 * Displays a single day's session schedule.
 */
class ScheduleFragment : RowsSupportFragment() {

    @Inject
    lateinit var viewModelFactory: ScheduleViewModelFactory

    private lateinit var viewModel: ScheduleViewModel

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    private val spinnerFragment = SpinnerFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (context?.applicationContext as TvApplication).scheduleComponent
                .inject(scheduleFragment = this)

        adapter = rowsAdapter

        fragmentManager?.inTransaction {
            add(R.id.main_frame, spinnerFragment)
        }

        viewModel = viewModelProvider(viewModelFactory)

        observeViewModel(viewModel)
    }

    override fun onStop() {
        super.onStop()
        fragmentManager?.inTransaction {
            remove(spinnerFragment)
        }
    }

    private fun loadAdapter(sessions: List<Session>) {

        val rows = mutableListOf<ListRow>()

        if (sessions.isEmpty()) {
            // TODO: replace with real UI once we have mocks.
            val dummyheader = HeaderItem(-1, getString(R.string.no_sessions_available))
            val dummyAdapter = ArrayObjectAdapter(object : Presenter() {
                override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
                    return ViewHolder(ImageCardView(parent?.context))
                }

                override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
                    val cardView = viewHolder?.view as ImageCardView

                    // TODO: replace with actual error message wording
                    cardView.titleText = getString(R.string.try_later)
                    cardView.contentText = getString(R.string.sorry_for_the_troubles)

                    // Set the image card's height and width.
                    val resources = cardView.context.resources
                    val cardWidth = resources.getDimensionPixelSize(R.dimen.card_width)
                    val cardHeight = resources.getDimensionPixelSize(R.dimen.card_height)
                    cardView.setMainImageDimensions(cardWidth, cardHeight)
                }

                override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}
            }).apply { add(Any()) }
            val dummyRow = ListRow(dummyheader, dummyAdapter)
            rows.add(dummyRow)
        } else {
            // TODO: replace with real data.
            val sessionAdapter = sessions.toArrayObjectAdapter(SessionPresenter())

            val firstSession = sessions[0]
            // TODO: move logic to format header string to ViewModel
            val header = TimeUtils.timeString(firstSession.startTime, firstSession.endTime)

            val dummyheader = HeaderItem(1, header)
            val dummyRow = ListRow(dummyheader, sessionAdapter)
            rows.add(dummyRow)
        }

        // TODO: move this DiffCallback implementation to it's own class/file once the algorithm for
        // comparing rows in this fragment is more concrete.
        rowsAdapter.setItems(rows, object : DiffCallback<ListRow>() {
            override fun areItemsTheSame(oldRow: ListRow, newRow: ListRow): Boolean {
                return TextUtils.equals(oldRow.contentDescription, newRow.contentDescription)
            }

            override fun areContentsTheSame(oldItem: ListRow, newItem: ListRow): Boolean {
                val oldAdapter = oldItem.adapter
                val newAdapter = newItem.adapter
                val sameSize = oldAdapter.size() == newAdapter.size()
                if (!sameSize) {
                    return false
                }

                for (i in 0 until oldAdapter.size()) {
                    val oldSession = oldAdapter.get(i) as Session
                    val newSession = newAdapter.get(i) as Session

                    if (!TextUtils.equals(oldSession.id, newSession.id)) {
                        return false
                    }
                }

                return true
            }
        })
        mainFragmentAdapter.fragmentHost.notifyDataReady(mainFragmentAdapter)
    }

    private fun observeViewModel(viewModel: ScheduleViewModel) {

        // Update text if there are sessions available
        viewModel.sessions.observe(this, Observer { sessions ->
            loadAdapter(sessions ?: emptyList())
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
