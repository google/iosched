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

package com.google.samples.apps.iosched.tv.ui.search

import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.content.IntentCompat.EXTRA_START_PLAYBACK
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.tv.TvApplication
import com.google.samples.apps.iosched.tv.ui.MainActivity
import timber.log.Timber
import javax.inject.Inject

/**
 * Handles the intent from a global search.
 *
 * The assistant determines if the content should begin immediately, and lets us know with the
 * boolean extra value, [EXTRA_START_PLAYBACK].
 *
 * If EXTRA_START_PLAYBACK is *true*, then launches [TODO_PlaybackActivity], otherwise, launches
 * [TODO_SessionDetailsActivity].
 */
class SearchableActivity : FragmentActivity() {

    @Inject
    lateinit var viewModelFactory: SearchableViewModelFactory

    private lateinit var viewModel: SearchableViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as TvApplication).searchableComponent.inject(searchableActivity = this)

        viewModel = viewModelProvider(viewModelFactory)

        val uri = if (intent == null || intent.data == null) {
            // Invalid intent and data supplied, quiting activity.
            finish()
            return
        } else {
            intent.data
        }

        Timber.d("Search data " + uri)
        val id = uri.lastPathSegment

        val startPlayback = intent.getBooleanExtra(EXTRA_START_PLAYBACK, false)
        Timber.d("Should start playback? ${if (startPlayback) "yes" else "no"}")

        if (startPlayback) {
            // TODO: start playback.
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            viewModel.session.observe(
                    this, Observer { _ ->
                // TODO: open session details. May require VM/UseCase to search for session by id.
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            )
            viewModel.loadSessionById(id)
        }
    }
}
