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
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFeedAnnouncementBinding
import com.google.samples.apps.iosched.databinding.ItemFeedAnnouncementsHeaderBinding
import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/** A data class representing the state of the announcements header card on feed */
data class AnnouncementsHeader(
    val showPastNotificationsButton: Boolean
)

class AnnouncementsHeaderViewBinder(
    private val lifecycleOwner: LifecycleOwner,
    private val eventListener: FeedEventListener
) : FeedItemViewBinder<AnnouncementsHeader, AnnouncementsPreviewViewHolder>(
    AnnouncementsHeader::class.java
) {

    override fun createViewHolder(parent: ViewGroup) =
        AnnouncementsPreviewViewHolder(
            ItemFeedAnnouncementsHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            lifecycleOwner,
            eventListener
        )

    override fun bindViewHolder(
        model: AnnouncementsHeader,
        viewHolder: AnnouncementsPreviewViewHolder
    ) = viewHolder.bind(model)

    override fun getFeedItemType() = R.layout.item_feed_announcements_header

    override fun areItemsTheSame(
        oldItem: AnnouncementsHeader,
        newItem: AnnouncementsHeader
    ) = true

    override fun areContentsTheSame(
        oldItem: AnnouncementsHeader,
        newItem: AnnouncementsHeader
    ) = oldItem == newItem
}

class AnnouncementsPreviewViewHolder(
    private val binding: ItemFeedAnnouncementsHeaderBinding,
    private val lifecycleOwner: LifecycleOwner,
    private val eventListener: FeedEventListener
) : ViewHolder(binding.root) {

    fun bind(model: AnnouncementsHeader) {
        binding.announcementsHeaderState = model
        binding.lifecycleOwner = lifecycleOwner
        binding.eventListener = eventListener
    }
}

// For Announcement items
class AnnouncementViewBinder(
    private val timeZoneId: LiveData<ZoneId>,
    private val lifecycleOwner: LifecycleOwner
) : FeedItemViewBinder<Announcement, AnnouncementViewHolder>(
    Announcement::class.java
) {

    override fun createViewHolder(parent: ViewGroup): AnnouncementViewHolder =
        AnnouncementViewHolder(
            ItemFeedAnnouncementBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            lifecycleOwner,
            timeZoneId
        )

    override fun bindViewHolder(model: Announcement, viewHolder: AnnouncementViewHolder) {
        viewHolder.bind(model)
    }

    override fun getFeedItemType(): Int = R.layout.item_feed_announcement

    override fun areItemsTheSame(oldItem: Announcement, newItem: Announcement): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Announcement, newItem: Announcement) =
        oldItem == newItem
}

class AnnouncementViewHolder(
    private val binding: ItemFeedAnnouncementBinding,
    private val lifecycleOwner: LifecycleOwner,
    private val timeZoneId: LiveData<ZoneId>
) : ViewHolder(binding.root) {

    fun bind(announcement: Announcement) {
        binding.announcement = announcement
        binding.lifecycleOwner = lifecycleOwner
        binding.timeZoneId = timeZoneId
        binding.executePendingBindings()
    }
}

// Shown while loading Announcements
object LoadingIndicator

class LoadingViewHolder(itemView: View) : ViewHolder(itemView)

class AnnouncementsLoadingViewBinder : FeedItemViewBinder<LoadingIndicator, LoadingViewHolder>(
    LoadingIndicator::class.java
) {

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return LoadingViewHolder(
            LayoutInflater.from(parent.context).inflate(getFeedItemType(), parent, false)
        )
    }

    override fun bindViewHolder(model: LoadingIndicator, viewHolder: LoadingViewHolder) {}

    override fun getFeedItemType() = R.layout.item_feed_announcements_loading

    override fun areItemsTheSame(oldItem: LoadingIndicator, newItem: LoadingIndicator) = true

    override fun areContentsTheSame(oldItem: LoadingIndicator, newItem: LoadingIndicator) = true
}

// Shown if there are no Announcements or fetching Announcments fails
object AnnouncementsEmpty

class EmptyViewHolder(itemView: View) : ViewHolder(itemView)

class AnnouncementsEmptyViewBinder : FeedItemViewBinder<AnnouncementsEmpty, EmptyViewHolder>(
    AnnouncementsEmpty::class.java
) {

    override fun createViewHolder(parent: ViewGroup): ViewHolder {
        return EmptyViewHolder(
            LayoutInflater.from(parent.context).inflate(getFeedItemType(), parent, false)
        )
    }

    override fun bindViewHolder(model: AnnouncementsEmpty, viewHolder: EmptyViewHolder) {}

    override fun getFeedItemType() = R.layout.item_feed_announcements_empty

    override fun areItemsTheSame(oldItem: AnnouncementsEmpty, newItem: AnnouncementsEmpty) = true

    override fun areContentsTheSame(oldItem: AnnouncementsEmpty, newItem: AnnouncementsEmpty) = true
}

@BindingAdapter("announcementTime", "timeZoneId")
fun announcementTime(
    textView: TextView,
    announcementTime: ZonedDateTime,
    timeZoneId: ZoneId?
) {
    timeZoneId ?: return
    // TODO(gauravbhola): Show relative time '2 minutes ago', '1 hour ago' etc
    textView.text =
        TimeUtils.abbreviatedTimeForAnnouncements(TimeUtils.zonedTime(announcementTime, timeZoneId))
}
