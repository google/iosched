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
import android.widget.LinearLayout
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemSpeakerDetailBinding
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.util.SpeakerUtils

@Suppress("unused")
@BindingAdapter("sessionSpeakers")
fun sessionSpeakers(layout: LinearLayout, speakers: Set<Speaker>) {
    layout.removeAllViews()
    SpeakerUtils.alphabeticallyOrderedSpeakerList(speakers).forEach {
        layout.addView(createSessionSpeakerView(layout, it))
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
