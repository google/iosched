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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.DetailsOverviewLogoPresenter
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.Presenter
import com.google.samples.apps.iosched.tv.R
import com.google.samples.apps.iosched.tv.ui.schedule.ScheduleFragment
import com.google.samples.apps.iosched.tv.ui.sessiondetail.SessionDetailFragment

/**
 * Displays a logo image next to the actions. This class is also responsible for transitioning the
 * image from the card in [ScheduleFragment] to the logo on the [SessionDetailFragment]
 */
class SessionDetailsLogoPresenter : DetailsOverviewLogoPresenter() {

    internal class ViewHolder(view: View) : DetailsOverviewLogoPresenter.ViewHolder(view) {

        override fun getParentPresenter(): FullWidthDetailsOverviewRowPresenter {
            return mParentPresenter
        }

        override fun getParentViewHolder(): FullWidthDetailsOverviewRowPresenter.ViewHolder {
            return mParentViewHolder
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup): Presenter.ViewHolder {
        val imageView = LayoutInflater.from(parent.context)
            .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false) as ImageView

        val res = parent.resources
        val width = res.getDimensionPixelSize(R.dimen.detail_thumb_width)
        val height = res.getDimensionPixelSize(R.dimen.detail_thumb_height)
        imageView.layoutParams = ViewGroup.MarginLayoutParams(width, height)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        return ViewHolder(imageView)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val row = item as DetailsOverviewRow
        val imageView = viewHolder.view as ImageView
        imageView.setImageDrawable(row.imageDrawable)
        if (isBoundToImage(viewHolder as ViewHolder, row)) {
            viewHolder.parentPresenter.notifyOnBindLogo(viewHolder.parentViewHolder)
        }
    }
}
