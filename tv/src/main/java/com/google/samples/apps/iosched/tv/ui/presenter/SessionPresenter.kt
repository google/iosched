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

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.tv.R

/**
 * Leanback presenter for displaying session information in a card view. Displays an individual
 * session from a list of sessions.
 */
class SessionPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val themedContext = ContextThemeWrapper(parent?.context, R.style.IOImageCardViewStyle)
        return ViewHolder(ImageCardView(themedContext))
    }

    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {

        val session = item as Session

        val cardView = viewHolder?.view as ImageCardView
        val context = cardView.context

        cardView.titleText = session.title

        // Set the image card's height and width.
        val resources = context.resources
        val cardWidth = resources.getDimensionPixelSize(R.dimen.card_width)
        val cardHeight = resources.getDimensionPixelSize(R.dimen.card_height)
        cardView.setMainImageDimensions(cardWidth, cardHeight)

        Glide.with(context)
            .load(session.photoUrl)
            .into(object : SimpleTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    cardView.mainImage = resource
                }
            })

        if (session.isLive()) {
            cardView.badgeImage = ContextCompat.getDrawable(context, R.drawable.ic_livestreamed)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {}
}
