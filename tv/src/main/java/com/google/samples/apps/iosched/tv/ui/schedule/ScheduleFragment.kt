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

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.Presenter
import androidx.lifecycle.Observer
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.shared.util.getThemeColor
import com.google.samples.apps.iosched.shared.util.inTransaction
import com.google.samples.apps.iosched.shared.util.lazyFast
import com.google.samples.apps.iosched.tv.R
import com.google.samples.apps.iosched.tv.app
import com.google.samples.apps.iosched.tv.ui.SpinnerFragment
import com.google.samples.apps.iosched.tv.ui.presenter.SessionPresenter
import com.google.samples.apps.iosched.tv.ui.sessiondetail.SessionDetailActivity
import com.google.samples.apps.iosched.tv.util.toArrayObjectAdapter
import javax.inject.Inject

/**
 * Displays a single day's session schedule.
 */
class ScheduleFragment : RowsSupportFragment() {

    @Inject
    lateinit var viewModelFactory: ScheduleViewModelFactory

    private lateinit var viewModel: ScheduleViewModel

    private val conferenceDay: Int by lazyFast {
        val args = requireNotNull(arguments, { "Missing arguments!" })
        args.getInt(ARG_CONFERENCE_DAY)
    }

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    private lateinit var noSessionsRow: ListRow

    private val spinnerFragment = SpinnerFragment()

    private lateinit var defaultBackground: Drawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app().scheduleComponent.inject(scheduleFragment = this)

        val color = requireActivity().getThemeColor(R.attr.colorPrimaryDark, R.color.indigo_dark)
        defaultBackground = ColorDrawable(color)

        adapter = rowsAdapter

        fragmentManager?.inTransaction {
            replace(R.id.main_frame, spinnerFragment, TAG_LOADING_FRAGMENT)
        }

        viewModel = activityViewModelProvider(viewModelFactory)

        noSessionsRow = createNoSessionRow()

        setOnItemViewClickedListener { itemViewHolder, item, _, _ ->
            if (item is Session) {

                val cardView = itemViewHolder.view as ImageCardView

                val context = cardView.context
                val intent =
                    SessionDetailActivity.createIntent(context = context, sessionId = item.id)

                startActivity(intent)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.getSessionsGroupedByTimeForDay(ConferenceDays[conferenceDay])
            .observe(requireActivity(), Observer { map ->
                loadAdapter(sessionsByTimeSlot = map ?: emptyMap())
            })

        viewModel.isLoading.observe(this, Observer { isLoading ->

            if (isLoading == false) {
                fragmentManager?.inTransaction {
                    remove(spinnerFragment)
                }
            }
        })

        viewModel.errorMessage.observe(this, Observer { message ->
            // TODO: Change once there's a way to show errors to the user
            if (!message.isNullOrEmpty() && !viewModel.wasErrorMessageShown()) {
                // Prevent the message from showing more than once
                viewModel.onErrorMessageShown()
                Toast.makeText(this.context, message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun createNoSessionRow(): ListRow {
        val noSessionHeader = HeaderItem(-1, getString(R.string.no_sessions_available))
        val noSessionAdapter = ArrayObjectAdapter(object : Presenter() {
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

                cardView.mainImage = defaultBackground
            }

            override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}
        }).apply { add(Any()) }

        return ListRow(noSessionHeader, noSessionAdapter)
    }

    private fun loadAdapter(sessionsByTimeSlot: Map<String, List<UserSession>>) {

        val rows = mutableListOf<ListRow>()

        if (sessionsByTimeSlot.isEmpty()) {
            rows.add(noSessionsRow)
        } else {
            for (timeSlot in sessionsByTimeSlot) {
                val header = HeaderItem(timeSlot.key)
                // TODO: use UserSession instead of plain session
                val sessions = timeSlot.value.map { it -> it.session }
                val sessionAdapter = sessions.toArrayObjectAdapter(SessionPresenter())
                val timeSlotRow = ListRow(header, sessionAdapter)
                rows.add(timeSlotRow)
            }
        }

        rowsAdapter.setItems(rows, TimeSlotSessionDiffCallback())
        mainFragmentAdapter.fragmentHost.notifyDataReady(mainFragmentAdapter)
    }

    companion object {

        const val ARG_CONFERENCE_DAY = "com.google.samples.apps.iosched.tv.ARG_CONFERENCE_DAY"
        const val TAG_LOADING_FRAGMENT = "tag_loading_fragment"

        fun newInstance(day: Int): ScheduleFragment {
            val args = Bundle().apply {
                putInt(ARG_CONFERENCE_DAY, day)
            }
            return ScheduleFragment().apply { arguments = args }
        }
    }
}
