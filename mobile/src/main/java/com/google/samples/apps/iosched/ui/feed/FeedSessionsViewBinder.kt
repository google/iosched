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

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
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
    val timeZoneId: ZoneId = ZoneId.systemDefault()
)

class FeedSessionsViewBinder(private val eventListener: FeedEventListener) :
    FeedListItemViewBinder<FeedSessions, FeedSessionsViewHolder>(FeedSessions::class.java) {

    override fun createViewHolder(parent: ViewGroup): FeedSessionsViewHolder =
        FeedSessionsViewHolder(
            ItemFeedSessionsContainerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            eventListener
        )

    override fun bindViewHolder(model: FeedSessions, viewHolder: FeedSessionsViewHolder) {
        viewHolder.bind(model)
    }

    override fun getFeedItemType(): Int = R.layout.item_feed_sessions_container

    override fun areItemsTheSame(oldItem: FeedSessions, newItem: FeedSessions): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: FeedSessions, newItem: FeedSessions) =
        oldItem == newItem
}

class FeedSessionsViewHolder(
    private val binding: ItemFeedSessionsContainerBinding,
    private val eventListener: FeedEventListener
) :
    ViewHolder(binding.root) {

    fun bind(sessions: FeedSessions) {
        binding.sessionContainerState = sessions
        binding.eventListener = eventListener
        val sessionAdapter =
            FeedSessionAdapter(eventListener = eventListener, timeZoneId = sessions.timeZoneId)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
            adapter = sessionAdapter
        }

        binding.actionButton.setOnClickListener {
            if (sessions.actionTextId == R.string.feed_view_all_events) {
                eventListener.openSchedule(false)
            } else {
                eventListener.openSchedule(true)
            }
        }
        sessionAdapter.submitList(sessions.userSessions)
        binding.executePendingBindings()
    }
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
