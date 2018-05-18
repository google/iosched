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

package com.google.samples.apps.iosched.tv.ui.sessionplayer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.tv.app
import timber.log.Timber
import javax.inject.Inject

/**
 * Launches YouTube to play session video.
 *
 * This activity is used as a delegate for playback. There are several entry points into video
 * playback and a landing activity is required. Those entry points include session details, search,
 * home screen channels, etc.
 */
class SessionPlayerActivity : FragmentActivity() {

    private lateinit var viewModel: SessionPlayerViewModel
    @Inject lateinit var viewModelFactory: SessionPlayerViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app().sessionPlayerComponent.inject(sessionPlayerActivity = this)

        val sessionId = intent.extras.getString(EXTRA_SESSION_ID)

        viewModel = viewModelProvider(viewModelFactory)
        viewModel.loadSessionById(sessionId)

        viewModel.session.observe(this, EventObserver { session ->

            Timber.d("Launching session in YouTube")
            if (session.hasVideo()) {
                startActivity(Intent(Intent.ACTION_VIEW, session.youTubeUrl.toUri()))
            }
            // Always finish activity since we are delegating to another view.
            finish()
        })
    }

    companion object {

        private const val EXTRA_SESSION_ID = "SESSION_ID"

        fun createIntent(context: Context, sessionId: SessionId): Intent {
            return Intent(context, SessionPlayerActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
        }
    }
}
