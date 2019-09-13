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

package com.google.samples.apps.iosched.shared.fcm

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.samples.apps.iosched.shared.data.work.ConferenceDataWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Receives Firebase Cloud Messages and schedule a [ConferenceDataWorker] to download new data.
 */
class IoschedFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Timber.d("Message data payload: ${remoteMessage?.data}")
        val data = remoteMessage?.data ?: return
        if (data[TRIGGER_EVENT_DATA_SYNC_key] == TRIGGER_EVENT_DATA_SYNC) {
            // Schedule a [ConferenceDataWorker] when FCM message with action
            // `TRIGGER_EVENT_DATA_SYNC` is received.
            scheduleFetchEventData()
        }
    }

    private fun scheduleFetchEventData() {
        // TODO Move back to NetworkType.UNMETERED with a 15 minute timeout condition

        // Moving from NetworkType.UNMETERED to NetworkType.CONNECTED because WorkManager
        // doesn't expose JobScheduler deadline
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val conferenceDataWorker = OneTimeWorkRequestBuilder<ConferenceDataWorker>()
            .setInitialDelay(MINIMUM_LATENCY, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()

        val operation = WorkManager.getInstance(this)
            .enqueueUniqueWork(
                uniqueConferenceDataWorker,
                ExistingWorkPolicy.KEEP,
                conferenceDataWorker)
            .result

        operation.addListener(
            { Timber.i("ConferenceDataWorker enqueued..") },
            { it.run() }
        )
    }

    companion object {
        private const val TRIGGER_EVENT_DATA_SYNC = "SYNC_EVENT_DATA"
        private const val TRIGGER_EVENT_DATA_SYNC_key = "action"

        // Some latency to avoid load spikes
        private const val MINIMUM_LATENCY = 5L

        // WorkManager UniqueWork string
        private const val uniqueConferenceDataWorker = "UNIQUECONFERENCEDATAWORKER"
    }
}
