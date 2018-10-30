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

package com.google.samples.apps.adssched.ui.sessiondetail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.firebase.ui.auth.IdpResponse
import com.google.samples.apps.adssched.R
import com.google.samples.apps.adssched.model.SessionId
import com.google.samples.apps.adssched.shared.notifications.AlarmBroadcastReceiver.Companion.QUERY_SESSION_ID
import com.google.samples.apps.adssched.shared.util.inTransaction
import com.google.samples.apps.adssched.ui.SnackbarMessage
import com.google.samples.apps.adssched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.adssched.util.signin.FirebaseAuthErrorCodeConverter
import dagger.android.support.DaggerAppCompatActivity
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class SessionDetailActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var snackbarMessageManager: SnackbarMessageManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_detail)

        val sessionId = getSessionId(intent)
        if (sessionId == null) {
            Timber.e("Session ID not specified")
            finish()
        } else {
            if (savedInstanceState == null) {
                supportFragmentManager.inTransaction {
                    add(R.id.session_detail_container, SessionDetailFragment.newInstance(sessionId))
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_CANCELED) {
            Timber.d("An activity returned RESULT_CANCELED")
            val response = IdpResponse.fromResultIntent(data)
            response?.error?.let {
                snackbarMessageManager.addMessage(
                    SnackbarMessage(
                        messageId = FirebaseAuthErrorCodeConverter.convert(it.errorCode),
                        requestChangeId = UUID.randomUUID().toString()
                    )
                )
            }
        }
    }

    private fun getSessionId(intent: Intent): String? {
        return intent.data?.getQueryParameter(QUERY_SESSION_ID) // for adssched://sessions/{id}
                ?: intent.getStringExtra(EXTRA_SESSION_ID)
    }

    companion object {
        private const val EXTRA_SESSION_ID = "SESSION_ID"

        fun starterIntent(context: Context, sessionId: SessionId): Intent {
            return Intent(context, SessionDetailActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                // Add this flag to avoid big back stacks
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }
}
