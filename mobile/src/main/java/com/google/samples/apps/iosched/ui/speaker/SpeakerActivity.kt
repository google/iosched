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

package com.google.samples.apps.iosched.ui.speaker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.shared.util.inTransaction
import dagger.android.support.DaggerAppCompatActivity

internal const val SPEAKER_ID = "speaker_id"

class SpeakerActivity : DaggerAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speaker)

        if (savedInstanceState == null) {
            supportFragmentManager.inTransaction {
                val speakerId = intent.getStringExtra(SPEAKER_ID)
                add(R.id.container, SpeakerFragment.newInstance(speakerId))
            }
        }
    }

    companion object {
        fun starterIntent(context: Context, speakerId: SpeakerId): Intent {
            return Intent(context, SpeakerActivity::class.java).apply {
                putExtra(SPEAKER_ID, speakerId)
            }
        }
    }
}
