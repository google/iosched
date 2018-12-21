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

package com.google.samples.apps.iosched.ui.agenda

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.BindingAdapter
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

private val agendaTimePattern = DateTimeFormatter.ofPattern("h:mm a")

@BindingAdapter(
    value = ["agendaColor", "agendaStrokeColor", "agendaStrokeWidth"], requireAll = true
)
fun agendaColor(view: View, fillColor: Int, strokeColor: Int, strokeWidth: Float) {
    view.background = (view.background as? GradientDrawable ?: GradientDrawable()).apply {
        setColor(fillColor)
        setStroke(strokeWidth.toInt(), strokeColor)
    }
}

@BindingAdapter("agendaIcon")
fun agendaIcon(imageView: ImageView, type: String) {
    val iconId = when (type) {
        "after_hours" -> R.drawable.ic_agenda_after_hours
        "badge" -> R.drawable.ic_agenda_badge
        "codelab" -> R.drawable.ic_agenda_codelab
        "concert" -> R.drawable.ic_agenda_concert
        "keynote" -> R.drawable.ic_agenda_keynote
        "meal" -> R.drawable.ic_agenda_meal
        "office_hours" -> R.drawable.ic_agenda_office_hours
        "sandbox" -> R.drawable.ic_agenda_sandbox
        "store" -> R.drawable.ic_agenda_store
        else -> R.drawable.ic_agenda_session
    }
    imageView.setImageDrawable(AppCompatResources.getDrawable(imageView.context, iconId))
}

@BindingAdapter(value = ["startTime", "endTime", "timeZoneId"], requireAll = true)
fun agendaDuration(
    textView: TextView,
    startTime: ZonedDateTime,
    endTime: ZonedDateTime,
    timeZoneId: ZoneId
) {
    textView.text = textView.context.getString(
        R.string.agenda_duration,
        agendaTimePattern.format(TimeUtils.zonedTime(startTime, timeZoneId)),
        agendaTimePattern.format(TimeUtils.zonedTime(endTime, timeZoneId))
    )
}
