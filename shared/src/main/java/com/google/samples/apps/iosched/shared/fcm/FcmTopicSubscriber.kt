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

import com.google.firebase.messaging.FirebaseMessaging
import timber.log.Timber

/**
 * A [TopicSubscriber] that uses Firebase Cloud Messaging to subscribe a user to server topics.
 *
 * Calls are lightweight and can be repeated multiple times.
 */
class FcmTopicSubscriber : TopicSubscriber {
    override fun subscribeToScheduleUpdates() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(CONFERENCE_DATA_UPDATE_TOPIC_KEY)
        } catch (e: Exception) {
            Timber.e(e, "Error subscribing to conference data update topic")
        }
    }

    companion object {
        private const val CONFERENCE_DATA_UPDATE_TOPIC_KEY = "ADSSCHED_DATA_SYNC_2019"
    }
}
