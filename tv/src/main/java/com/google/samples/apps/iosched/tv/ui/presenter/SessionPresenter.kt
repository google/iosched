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

import android.support.v17.leanback.widget.ImageCardView
import android.support.v17.leanback.widget.Presenter
import android.view.ViewGroup
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.tv.R

/**
 * Leanback presenter for displaying session information in a card view. Displays an individual
 * session from a list of sessions.
 */
class SessionPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        // TODO: Create a better view based on the mocks
        val cardView = ImageCardView(parent?.context)

        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {

        val session = item as Session

        val cardView = viewHolder?.view as ImageCardView

        cardView.titleText = session.title
        cardView.contentText = TimeUtils.timeString(session.startTime, session.endTime)

        // Set the image card's height and width.
        val resources = cardView.context.resources
        val cardWidth = resources.getDimensionPixelSize(R.dimen.card_width)
        val cardHeight = resources.getDimensionPixelSize(R.dimen.card_height)
        cardView.setMainImageDimensions(cardWidth, cardHeight)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}
}