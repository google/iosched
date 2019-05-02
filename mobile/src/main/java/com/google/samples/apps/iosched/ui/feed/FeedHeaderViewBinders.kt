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
import android.widget.Button
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.button.MaterialButton
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFeedMomentBinding
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.model.Moment.Companion.CTA_LIVE_STREAM
import com.google.samples.apps.iosched.model.Moment.Companion.CTA_MAP_LOCATION
import com.google.samples.apps.iosched.model.Moment.Companion.CTA_SIGNIN
import com.google.samples.apps.iosched.model.Theme
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.util.executeAfter

// Countdown is shown before the Keynote
object CountdownItem

class CountdownViewHolder(itemView: View) : ViewHolder(itemView)

class CountdownViewBinder : FeedItemViewBinder<CountdownItem, CountdownViewHolder>(
    CountdownItem::class.java
) {

    override fun createViewHolder(parent: ViewGroup): CountdownViewHolder {
        return CountdownViewHolder(
            LayoutInflater.from(parent.context).inflate(getFeedItemType(), parent, false)
        )
    }

    override fun bindViewHolder(model: CountdownItem, viewHolder: CountdownViewHolder) {}

    override fun getFeedItemType() = R.layout.item_feed_countdown

    override fun areItemsTheSame(oldItem: CountdownItem, newItem: CountdownItem) = true

    override fun areContentsTheSame(oldItem: CountdownItem, newItem: CountdownItem) = true
}

// A Moment may be shown during or after the conference based on the current time
class MomentViewBinder(
    private val eventListener: FeedEventListener,
    private val userInfoLiveData: LiveData<AuthenticatedUserInfo?>,
    private val themeLiveData: LiveData<Theme>
) : FeedItemViewBinder<Moment, MomentViewHolder>(Moment::class.java) {

    override fun createViewHolder(parent: ViewGroup): MomentViewHolder {
        val binding =
            ItemFeedMomentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MomentViewHolder(binding, eventListener, userInfoLiveData, themeLiveData)
    }

    override fun bindViewHolder(model: Moment, viewHolder: MomentViewHolder) {
        viewHolder.bind(model)
    }

    override fun getFeedItemType(): Int = R.layout.item_feed_moment

    override fun areItemsTheSame(oldItem: Moment, newItem: Moment): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Moment, newItem: Moment) = oldItem == newItem
}

class MomentViewHolder(
    private val binding: ItemFeedMomentBinding,
    private val eventListener: FeedEventListener,
    private val userInfoLiveData: LiveData<AuthenticatedUserInfo?>,
    private val themeLiveData: LiveData<Theme>
) : ViewHolder(binding.root) {

    fun bind(item: Moment) {
        binding.executeAfter {
            moment = item
            theme = themeLiveData
            userInfo = userInfoLiveData
            eventListener = this@MomentViewHolder.eventListener
        }
    }
}

@BindingAdapter("moment", "userInfo", "eventListener")
fun setMomentActionButton(
    button: Button,
    moment: Moment?,
    userInfo: AuthenticatedUserInfo?,
    eventListener: FeedEventListener?
) {
    if (button !is MaterialButton) return
    moment ?: return
    eventListener ?: return
    val userSignedIn = userInfo?.isSignedIn() ?: false

    when (moment.ctaType) {
        CTA_LIVE_STREAM -> {
            val url = moment.streamUrl
            if (url.isNullOrEmpty()) {
                button.isVisible = false
            } else {
                button.apply {
                    isVisible = true
                    setText(R.string.feed_watch_live_stream)
                    setOnClickListener {
                        eventListener.openLiveStream(url)
                    }
                    setIconResource(R.drawable.ic_play_circle_outline)
                }
            }
        }
        CTA_MAP_LOCATION -> {
            button.apply {
                isVisible = true
                text = moment.featureName
                setOnClickListener {
                    eventListener.openMap(moment)
                }
                setIconResource(R.drawable.ic_nav_map)
            }
        }
        CTA_SIGNIN -> {
            if (userSignedIn) {
                button.isVisible = false
            } else {
                button.apply {
                    isVisible = true
                    setText(R.string.sign_in)
                    setOnClickListener { eventListener.signIn() }
                    setIconResource(R.drawable.ic_default_profile_avatar)
                }
            }
        }
        else /* CTA_NO_ACTION */ -> button.isVisible = false
    }
}
