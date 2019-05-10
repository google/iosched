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

import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFeedSessionBinding
import com.google.samples.apps.iosched.databinding.ItemFeedSessionsContainerBinding
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.ui.schedule.SessionDiff
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/** A data class representing the state of FeedSEssionsContainer */
data class FeedSessions(
    val id: String = "sessions_container",
    val username: String?,
    @StringRes val titleId: Int,
    @StringRes val actionTextId: Int,
    val userSessions: List<UserSession>,
    val timeZoneId: ZoneId = ZoneId.systemDefault(),
    val isLoading: Boolean
)

class FeedSessionsViewBinder(
    private val eventListener: FeedEventListener,
    var recyclerViewManagerState: Parcelable? = null
) :
    FeedListItemViewBinder<FeedSessions, FeedSessionsViewHolder>(FeedSessions::class.java) {

    override fun createViewHolder(parent: ViewGroup): FeedSessionsViewHolder {
        val holder = FeedSessionsViewHolder(
            ItemFeedSessionsContainerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            eventListener
        )

        holder.binding.recyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    saveInstanceState(holder)
                }
            }
        })
        return holder
    }

    override fun bindViewHolder(model: FeedSessions, viewHolder: FeedSessionsViewHolder) {
        viewHolder.bind(model, recyclerViewManagerState)
    }

    override fun getFeedItemType(): Int = R.layout.item_feed_sessions_container

    override fun areItemsTheSame(oldItem: FeedSessions, newItem: FeedSessions): Boolean = true

    override fun areContentsTheSame(oldItem: FeedSessions, newItem: FeedSessions) =
        oldItem == newItem

    override fun onViewRecycled(viewHolder: FeedSessionsViewHolder) {
        saveInstanceState(viewHolder)
    }

    override fun onViewDetachedFromWindow(viewHolder: FeedSessionsViewHolder) {
        saveInstanceState(viewHolder)
    }

    fun saveInstanceState(viewHolder: FeedSessionsViewHolder) {
        if (viewHolder.adapterPosition == NO_POSITION) {
            return
        }
        recyclerViewManagerState = viewHolder.getLayoutManagerState()
    }
}

class FeedSessionsViewHolder(
    val binding: ItemFeedSessionsContainerBinding,
    private val eventListener: FeedEventListener
) :
    ViewHolder(binding.root) {

    private var layoutManager: LayoutManager? = null

    fun bind(sessions: FeedSessions, layoutManagerState: Parcelable?) {
        binding.sessionContainerState = sessions
        binding.eventListener = eventListener
        val sessionAdapter =
            FeedSessionAdapter(eventListener = eventListener, timeZoneId = sessions.timeZoneId)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
            adapter = sessionAdapter
        }
        layoutManager = binding.recyclerView.layoutManager

        binding.actionButton.setOnClickListener {
            if (sessions.actionTextId == R.string.feed_view_all_events) {
                eventListener.openSchedule(false)
            } else {
                eventListener.openSchedule(true)
            }
        }
        sessionAdapter.submitList(sessions.userSessions)
        if (layoutManagerState != null) {
            layoutManager?.onRestoreInstanceState(layoutManagerState)
        }

        binding.executePendingBindings()
    }

    fun getLayoutManagerState(): Parcelable? = layoutManager?.onSaveInstanceState()
}

/** Adapter which provides views for sessions inside the FeedSessionsContainer */
class FeedSessionAdapter(
    private val eventListener: FeedEventListener,
    private val timeZoneId: ZoneId
) :
    ListAdapter<UserSession, FeedSessionItemViewHolder>(SessionDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedSessionItemViewHolder {
        val binding = ItemFeedSessionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FeedSessionItemViewHolder(binding, eventListener, timeZoneId)
    }

    override fun onBindViewHolder(holder: FeedSessionItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

/** ViewHolder for the sessions inside the FeedSessionsContainer */
class FeedSessionItemViewHolder(
    private val binding: ItemFeedSessionBinding,
    private val eventListener: FeedEventListener,
    private val timeZoneId: ZoneId
) : ViewHolder(binding.root) {
    fun bind(userSession: UserSession) {
        binding.userSession = userSession
        binding.eventListener = eventListener
        binding.timeZoneId = timeZoneId
    }
}

@BindingAdapter("startTime", "timeZoneId")
fun sessionStartTime(
    textView: TextView,
    startTime: ZonedDateTime,
    timeZoneId: ZoneId
) {
    val timePattern = DateTimeFormatter.ofPattern("h:mm a")
    textView.text =
        timePattern.format(TimeUtils.zonedTime(startTime, zoneId = timeZoneId))
}
