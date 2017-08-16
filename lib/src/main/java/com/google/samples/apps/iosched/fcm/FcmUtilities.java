/*
 * Copyright 2017 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.fcm;

import com.google.firebase.messaging.FirebaseMessaging;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class FcmUtilities {
    private static final String TAG = makeLogTag(FcmUtilities.class);

    private static final String CONFERENCE_MESSAGES_TOPIC_ONSITE = "/topics/confmessagesonsite";
    private static final String CONFERENCE_MESSAGES_TOPIC_OFFSITE = "/topics/confmessagesoffsite";

    public static void subscribeTopics(boolean isConfMessageCardsEnabled, boolean isRegisteredAttendee) {
        try {
            FirebaseMessaging pubSub = FirebaseMessaging.getInstance();
            if (isConfMessageCardsEnabled) {
                if (isRegisteredAttendee) {
                    pubSub.unsubscribeFromTopic(CONFERENCE_MESSAGES_TOPIC_OFFSITE);
                    pubSub.subscribeToTopic(CONFERENCE_MESSAGES_TOPIC_ONSITE);
                } else {
                    pubSub.subscribeToTopic(CONFERENCE_MESSAGES_TOPIC_OFFSITE);
                    pubSub.unsubscribeFromTopic(CONFERENCE_MESSAGES_TOPIC_ONSITE);
                }
            } else {
                pubSub.unsubscribeFromTopic(CONFERENCE_MESSAGES_TOPIC_ONSITE);
                pubSub.unsubscribeFromTopic(CONFERENCE_MESSAGES_TOPIC_OFFSITE);
            }
        } catch (Throwable throwable) {
            // Just in case.
            LOGE(TAG, "Exception updating conference message cards subscription.", throwable);
        }
    }
}
