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

package com.google.samples.apps.iosched.ui.sessiondetail

import android.databinding.BindingAdapter
import android.databinding.DataBindingUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemSpeakerDetailBinding
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.util.SpeakerUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.util.drawable.HeaderGridDrawable
import org.threeten.bp.ZonedDateTime

@Suppress("unused")
@BindingAdapter("headerImage")
fun headerImage(imageView: ImageView, photoUrl: String?) {
    if (!photoUrl.isNullOrEmpty()) {
        // TODO get thumbnail
    } else {
        imageView.setImageDrawable(HeaderGridDrawable(imageView.context))
    }
}

@Suppress("unused")
@BindingAdapter("sessionSpeakers")
fun sessionSpeakers(layout: LinearLayout, speakers: Set<Speaker>?) {
    if (speakers != null) {
        // remove all views other than the header
        for (i in layout.childCount - 1 downTo 1) {
            layout.removeViewAt(i)
        }
        SpeakerUtils.alphabeticallyOrderedSpeakerList(speakers).forEach {
            layout.addView(createSessionSpeakerView(layout, it))
        }
    }
}

@Suppress("unused")
@BindingAdapter(value = ["sessionDetailStartTime", "sessionDetailEndTime"], requireAll = true)
fun timeString(
        view: TextView,
        sessionDetailStartTime: ZonedDateTime?,
        sessionDetailEndTime: ZonedDateTime?
) {
    if (sessionDetailStartTime == null || sessionDetailEndTime == null) {
        view.text = ""
    } else {
        view.text = TimeUtils.timeString(sessionDetailStartTime, sessionDetailEndTime)
    }
}

private fun createSessionSpeakerView(
    container: ViewGroup,
    speaker: Speaker
): View {
    val binding: ItemSpeakerDetailBinding = DataBindingUtil.inflate(
        LayoutInflater.from(container.context),
        R.layout.item_speaker_detail,
        container,
        false)

    binding.speaker = speaker
    return binding.root
}