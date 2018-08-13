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

package com.google.samples.apps.iosched.tv.ui.presenter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.leanback.widget.AbstractDetailsDescriptionPresenter
import androidx.leanback.widget.Presenter
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.tv.R
import com.google.samples.apps.iosched.tv.ui.adapter.TagAdapter

/**
 * Custom presenter for displaying the details of a [Session].
 *
 * Extending [AbstractDetailsDescriptionPresenter] limits the display to title, subtitle,
 * description. This custom presenter displays title, abstract, speakers, tags, and other metadata
 * about a session.
 */
class DetailsDescriptionPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.session_details, parent, false)
        return SessionDetailViewHolder(v)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val session = item as Session
        val holder = viewHolder as SessionDetailViewHolder

        // TODO: move to data binding.
        holder.titleView.text = session.title
        holder.timeView.text = TimeUtils.timeString(session.startTime, session.endTime)
        holder.roomView.text = session.room?.name ?: ""
        holder.descriptionView.text = session.abstract

        holder.tagRecyclerView.adapter = TagAdapter().apply {
            tags = session.displayTags
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
    }
}
