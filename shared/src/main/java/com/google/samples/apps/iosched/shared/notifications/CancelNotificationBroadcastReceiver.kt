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

package com.google.samples.apps.iosched.shared.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Receives broadcast intents with information to hide notifications.
 */
@AndroidEntryPoint
class CancelNotificationBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_ID_EXTRA = "notification_id_extra"
    }

    @Inject
    @ApplicationContext
    lateinit var context: Context

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(
            NOTIFICATION_ID_EXTRA, 0)
        Timber.d("Hiding notification for $notificationId")

        val notificationManger: NotificationManager = context.getSystemService()
            ?: throw Exception("Notification Manager not found.")

        notificationManger.cancel(notificationId)
    }
}
