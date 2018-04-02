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

package com.google.samples.apps.iosched.wear.ui.sessiondetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.samples.apps.iosched.shared.util.inTransaction
import com.google.samples.apps.iosched.wear.R
import dagger.android.support.DaggerAppCompatActivity

/**
 * Displays Session details appropriate for Wearable device.
 */
class SessionDetailActivity : DaggerAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_detail)

        if (savedInstanceState == null) {
            supportFragmentManager.inTransaction {
                val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
                add(R.id.session_detail_container, SessionDetailFragment.newInstance(sessionId))
            }
        }
    }

    companion object {
        private const val EXTRA_SESSION_ID = "SESSION_ID"

        fun starterIntent(context: Context, sessionId: String): Intent {
            return Intent(context, SessionDetailActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
        }
    }
}
