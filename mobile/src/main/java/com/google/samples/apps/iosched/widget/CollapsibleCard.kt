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

package com.google.samples.apps.iosched.widget

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import android.support.v7.content.res.AppCompatResources
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.samples.apps.iosched.R

class CollapsibleCard @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var expanded = false
    private val cardTitleView: TextView
    private val cardDescriptionView: HtmlTextView
    private val expandIcon: ImageView
    private val titleContainer: View

    init {
        val arr = context.obtainStyledAttributes(attrs, R.styleable.CollapsibleCard, 0, 0)
        val cardTitle = arr.getString(R.styleable.CollapsibleCard_cardTitle)
        val cardDescription = arr.getString(R.styleable.CollapsibleCard_cardDescription)
        arr.recycle()
        val root = LayoutInflater.from(context)
                .inflate(R.layout.collapsible_card_content, this, true)

        titleContainer = root.findViewById(R.id.title_container)
        cardTitleView = root.findViewById<TextView>(R.id.card_title).apply {
            text = cardTitle
        }
        setTitleContentDescription(cardTitle)
        cardDescriptionView = root.findViewById<HtmlTextView>(R.id.card_description).apply {
            text = cardDescription
        }
        expandIcon = root.findViewById(R.id.expand_icon)
        if (SDK_INT < M) {
            expandIcon.imageTintList =
                    AppCompatResources.getColorStateList(context, R.color.collapsing_section)
        }
        val toggle = TransitionInflater.from(context)
                .inflateTransition(R.transition.info_card_toggle)
        titleContainer.setOnClickListener {
            expanded = !expanded
            toggle.duration = if (expanded) 300L else 200L
            TransitionManager.beginDelayedTransition(root.parent as ViewGroup, toggle)
            cardDescriptionView.visibility = if (expanded) View.VISIBLE else View.GONE
            expandIcon.rotation = if (expanded) 180f else 0f
            // activated used to tint controls when expanded
            expandIcon.isActivated = expanded
            cardTitleView.isActivated = expanded
            setTitleContentDescription(cardTitle)
        }
    }

    private fun setTitleContentDescription(cardTitle: String?) {
        val res = resources
        cardTitleView.contentDescription = "$cardTitle, " +
                if (expanded)
                    res.getString(R.string.expanded)
                else
                    res.getString(R.string.collapsed)
    }
}