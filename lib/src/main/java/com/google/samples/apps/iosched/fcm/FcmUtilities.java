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
