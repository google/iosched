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
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.util.drawable.HeaderGridDrawable

class EventView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onViewSessionsClicked: (view: View, uri: String) -> Unit = { _, _ -> Unit }
    var onViewMapClicked: (view: View, uri: String) -> Unit = { _, _ -> Unit }
    var onViewCodelabsClicked: (view: View, uri: String) -> Unit = { _, _ -> Unit }

    init {
        val arr = context.obtainStyledAttributes(attrs, R.styleable.EventView, defStyleAttr, 0)
        val eventTitle = arr.getString(R.styleable.EventView_eventTitle)
        val eventDescription = arr.getString(R.styleable.EventView_eventDescription)
        val eventTitleBackground = arr.getDrawable(R.styleable.EventView_eventTypeLogo)
        val mapUri = arr.getString(R.styleable.EventView_mapLinkUri)
        val codelabsUri = arr.getString(R.styleable.EventView_codelabsUri)
        val filteredEventsUri = arr.getString(R.styleable.EventView_filteredEventsLinkUri)
        arr.recycle()

        val rootView = LayoutInflater.from(context)
            .inflate(R.layout.info_event_content_card_view, this, true)

        rootView.findViewById<ImageView>(R.id.grid).apply {
            setImageDrawable(HeaderGridDrawable(context))
        }

        rootView.findViewById<ImageView>(R.id.event_type_logo).apply {
            setImageDrawable(eventTitleBackground)
        }

        rootView.findViewById<TextView>(R.id.event_title).apply {
            text = eventTitle
        }
        rootView.findViewById<TextView>(R.id.event_content_description).apply {
            text = eventDescription
        }

        val viewSessions = rootView.findViewById<Button>(R.id.event_view_sessions)
        if (filteredEventsUri != null) {
            viewSessions.setOnClickListener {
                onViewSessionsClicked(this@EventView, filteredEventsUri)
            }
        } else {
            viewSessions.visibility = View.GONE
        }

        val viewMap = rootView.findViewById<Button>(R.id.event_view_map)
        if (mapUri != null) {
            viewMap.setOnClickListener {
                onViewMapClicked(this@EventView, mapUri)
            }
        } else {
            viewMap.visibility = View.GONE
        }

        val viewCodelabs = rootView.findViewById<Button>(R.id.event_view_codelabs)
        if (mapUri != null) {
            viewCodelabs.setOnClickListener {
                onViewCodelabsClicked(this@EventView, codelabsUri)
            }
        } else {
            viewCodelabs.visibility = View.GONE
        }
    }
}
