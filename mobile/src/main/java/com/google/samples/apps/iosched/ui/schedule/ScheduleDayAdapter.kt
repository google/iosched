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

package com.google.samples.apps.iosched.ui.schedule

import android.R.layout
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.util.inflate

class ScheduleDayAdapter : Adapter<SessionViewHolder>() {

    private var sessions: List<Session> = emptyList()

    fun setList(sessions: List<Session>) {
        // TODO diff
        this.sessions = sessions
        notifyDataSetChanged()
    }

    override fun getItemCount() = sessions.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        return SessionViewHolder(parent.inflate(layout.simple_list_item_1))
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(sessions[position])
    }
}

class SessionViewHolder(itemView: View) : ViewHolder(itemView) {
    val title: TextView = itemView.findViewById(android.R.id.text1)

    fun bind(session: Session) {
        title.text = session.title
    }
}
