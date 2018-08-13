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

import android.view.View
import android.widget.TextView
import androidx.leanback.widget.Presenter
import androidx.recyclerview.widget.RecyclerView
import com.google.samples.apps.iosched.tv.R

/**
 * [DetailsDescriptionPresenter] creates and binds this session details view holder.
 */
class SessionDetailViewHolder(view: View?) : Presenter.ViewHolder(view) {

    val titleView: TextView
    val timeView: TextView
    val roomView: TextView
    val descriptionView: TextView
    val tagRecyclerView: RecyclerView

    init {
        val v = requireNotNull(view)

        titleView = v.findViewById(R.id.session_detail_title)
        timeView = v.findViewById(R.id.session_detail_time)
        roomView = v.findViewById(R.id.session_detail_room)
        descriptionView = v.findViewById(R.id.session_detail_description)
        tagRecyclerView = v.findViewById(R.id.tags)
    }
}
