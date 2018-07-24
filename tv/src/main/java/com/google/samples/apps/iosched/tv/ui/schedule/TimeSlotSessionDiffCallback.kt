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

package com.google.samples.apps.iosched.tv.ui.schedule

import android.text.TextUtils
import androidx.leanback.widget.DiffCallback
import androidx.leanback.widget.ListRow
import com.google.samples.apps.iosched.model.Session

/**
 * Compares if two [ListRow]s are the same.
 *
 * Determines if the rows are the same if their header text is equal.
 *
 * Determines if the contents in both rows are the same if each [Session] in one row has the same
 * [Session.id] as the session is the same position as the other row.
 */
class TimeSlotSessionDiffCallback : DiffCallback<ListRow>() {

    override fun areItemsTheSame(oldRow: ListRow, newRow: ListRow): Boolean {
        return TextUtils.equals(oldRow.contentDescription, newRow.contentDescription)
    }

    override fun areContentsTheSame(oldItem: ListRow, newItem: ListRow): Boolean {
        val oldAdapter = oldItem.adapter
        val newAdapter = newItem.adapter
        val sameSize = oldAdapter.size() == newAdapter.size()
        if (!sameSize) {
            return false
        }

        for (i in 0 until oldAdapter.size()) {
            if (oldAdapter.get(i) is Session && newAdapter.get(i) is Session) {
                val oldSession = oldAdapter.get(i) as Session
                val newSession = newAdapter.get(i) as Session

                if (!TextUtils.equals(oldSession.id, newSession.id)) {
                    return false
                }
            } else {
                return false
            }
        }

        return true
    }
}
