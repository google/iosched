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
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.google.samples.apps.iosched.R

class EventCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {

    init {
        val arr = context.obtainStyledAttributes(
            attrs, R.styleable.EventCardView, defStyleAttr, R.style.Widget_IOSched_EventCardView
        )
        val eventTitle = arr.getString(R.styleable.EventCardView_eventTitle)
        val eventDescription = arr.getString(R.styleable.EventCardView_eventDescription)
        val eventTypeLogo = arr.getDrawable(R.styleable.EventCardView_eventTypeLogo)
        val eventTypeLogoBg = arr.getDrawable(R.styleable.EventCardView_eventTypeLogoBackground)
        arr.recycle()

        LayoutInflater.from(context).inflate(R.layout.event_card_content, this, true)

        findViewById<ImageView>(R.id.header_image).apply {
            background = eventTypeLogoBg
            setImageDrawable(eventTypeLogo)
        }

        findViewById<TextView>(R.id.event_title).apply {
            text = eventTitle
        }
        findViewById<TextView>(R.id.event_description).apply {
            text = eventDescription
        }
    }
}
