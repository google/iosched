/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.messages;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.google.samples.apps.iosched.fcm.FcmUtilities;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.settings.ConfMessageCardUtils;
import com.google.samples.apps.iosched.settings.ConfMessageCardUtils.ConfMessageCard;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.RegistrationUtils;
import com.google.samples.apps.iosched.util.WiFiUtils;

import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Helper class to create message data view objects representing MessageCards
 */
public class MessageCardHelper {
    private static final String TAG = makeLogTag(MessageCardHelper.class);

    private static final String TWITTER_PACKAGE_NAME = "com.twitter.android";
    private static final String GPLUS_PACKAGE_NAME = "com.google.android.apps.plus";

    public static MessageData getSimpleMessageCardData(
      final ConfMessageCardUtils.ConfMessageCard card) {
        MessageData messageData = new MessageData();
        messageData.setMessage(card.getSimpleMessage());
        messageData.setEndButtonStringResourceId(R.string.ok);
        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                ConfMessageCardUtils.markDismissedConfMessageCard(v.getContext(), card);
            }
        });
        return messageData;
    }

    /**
     * Return the conference messages opt-in data.
     */
    public static MessageData getConferenceOptInMessageData() {
        MessageData messageData = new MessageData();
        messageData.setMessageStringResourceId(R.string.explore_io_msgcards_ask_opt_in);
        messageData.setStartButtonStringResourceId(R.string.explore_io_msgcards_answer_no);
        messageData.setEndButtonStringResourceId(R.string.explore_io_msgcards_answer_yes);

        messageData.setStartButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking conference messages question answered with decline.");
                ConfMessageCardUtils.markAnsweredConfMessageCardsPrompt(view.getContext(), true);
                ConfMessageCardUtils.setConfMessageCardsEnabled(view.getContext(), false);
                final Activity activity = getActivity(view);
                if (activity != null) {
                    // This will activate re-registering with the correct FCM topic(s).
                    FcmUtilities.subscribeTopics(
                            ConfMessageCardUtils.isConfMessageCardsEnabled(view.getContext()),
                            RegistrationUtils.isRegisteredAttendee(view.getContext()) ==
                                    RegistrationUtils.REGSTATUS_REGISTERED);
                }
            }
        });
        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking conference messages question answered with affirmation.");
                ConfMessageCardUtils.markAnsweredConfMessageCardsPrompt(view.getContext(), true);
                ConfMessageCardUtils.setConfMessageCardsEnabled(view.getContext(), true);
                final Activity activity = getActivity(view);
                if (activity != null) {
                    // This will activate re-registering with the correct FCM topic(s).
                    FcmUtilities.subscribeTopics(
                            ConfMessageCardUtils.isConfMessageCardsEnabled(view.getContext()),
                            RegistrationUtils.isRegisteredAttendee(view.getContext()) ==
                                    RegistrationUtils.REGSTATUS_REGISTERED);
                }
            }
        });

        return messageData;
    }

    /**
     * Return the notifications messages opt-in data.
     */
    public static MessageData getNotificationsOptInMessageData() {
        MessageData messageData = new MessageData();
        messageData.setMessageStringResourceId(R.string.explore_io_notifications_ask_opt_in);
        messageData.setStartButtonStringResourceId(R.string.explore_io_msgcards_answer_no);
        messageData.setEndButtonStringResourceId(R.string.explore_io_msgcards_answer_yes);

        messageData.setStartButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking notifications question answered with decline.");
                ConfMessageCardUtils.setDismissedConfMessageCard(view.getContext(),
                        ConfMessageCardUtils.ConfMessageCard.SESSION_NOTIFICATIONS, false);
                SettingsUtils.setShowNotifications(view.getContext(), false);
                SettingsUtils.setShowSessionFeedbackReminders(view.getContext(), false);
            }
        });
        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking notifications messages question answered with affirmation.");
                ConfMessageCardUtils.setDismissedConfMessageCard(view.getContext(),
                        ConfMessageCardUtils.ConfMessageCard.SESSION_NOTIFICATIONS, true);
                SettingsUtils.setShowNotifications(view.getContext(), true);
                SettingsUtils.setShowSessionFeedbackReminders(view.getContext(), true);
            }
        });

        return messageData;
    }

    /**
     * Return the wifi setup card data.
     */
    public static MessageData getWifiSetupMessageData() {
        MessageData messageData = new MessageData();
        messageData.setMessageStringResourceId(R.string.question_setup_wifi_card_text);
        messageData.setStartButtonStringResourceId(R.string.explore_io_msgcards_answer_no);
        messageData.setEndButtonStringResourceId(R.string.explore_io_msgcards_answer_yes);
        messageData.setIconDrawableId(R.drawable.message_card_wifi);

        messageData.setStartButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking wifi setup declined.");

                // Switching like this ensure the value change listener is fired.
                SettingsUtils.markDeclinedWifiSetup(view.getContext(), false);
                SettingsUtils.markDeclinedWifiSetup(view.getContext(), true);
            }
        });
        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Installing conference wifi.");
                WiFiUtils.installConferenceWiFi(view.getContext());

                // Switching like this ensure the value change listener is fired.
                SettingsUtils.markDeclinedWifiSetup(view.getContext(), true);
                SettingsUtils.markDeclinedWifiSetup(view.getContext(), false);
            }
        });

        return messageData;
    }

    /**
     * The card displayed on My I/O to a non-signed in user immediately after onboarding.
     */
    public static MessageData notSignedInCard() {
        MessageData messageData = new MessageData();
        messageData.setMessageStringResourceId(R.string.signin_message);
        messageData.setStartButtonStringResourceId(R.string.signin_prompt_skip);
        messageData.setEndButtonStringResourceId(R.string.signin_prompt);
        return messageData;
    }

    /**
     * The card displayed on My I/O to a signed in user immediately after onboarding.
     */
    public static MessageData signedInMessageCard() {
        MessageData messageData = new MessageData();
        messageData.setMessageStringResourceId(
                R.string.signed_in_card_message);
        messageData.setStartButtonStringResourceId(R.string.signin_prompt_dismiss);
        return messageData;
    }

    /**
     * Return whether a package is installed.
     */
    public static boolean isPackageInstalledAndEnabled(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info;
        try {
            info = pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            info = null;
        }

        return info != null &&
                info.applicationInfo != null &&
                info.applicationInfo.enabled;
    }

    @Nullable()
    private static Activity getActivity(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /**
     * @return the next message card that should be displayed to the user
     */
    public static MessageData getNextMessageCard(@NonNull Context context) {
        if (shouldShowCard(context, ConfMessageCard.SIGN_IN_PROMPT)) {
            return notSignedInCard();
        }
        if (shouldShowCard(context, ConfMessageCard.SESSION_NOTIFICATIONS)) {
            return getNotificationsOptInMessageData();
        }
        if (RegistrationUtils.isRegisteredAttendee(context) ==
                RegistrationUtils.REGSTATUS_REGISTERED) {
            // Users are required to opt in or out of whether they want conference message cards
            if (!ConfMessageCardUtils.hasAnsweredConfMessageCardsPrompt(context)) {
                // User has not answered whether they want to opt in.
                // Build a opt-in/out card.
                return getConferenceOptInMessageData();
            }

            if (ConfMessageCardUtils.isConfMessageCardsEnabled(context)) {
                LOGD(TAG, "Conf cards Enabled");
                // User has answered they want to opt in AND the message cards are enabled.
                ConfMessageCardUtils.enableActiveCards(context);

                // Note that for these special cards, we'll never show more than one at a time
                // to prevent overloading the user with messagesToDisplay.
                // We want each new message to be notable.
                if (shouldShowCard(context, ConfMessageCard.WIFI_PRELOAD)) {
                    // Check whether a wifi setup card should be offered.
                    if (WiFiUtils.shouldOfferToSetupWifi(context, true)) {
                        // Build card asking users whether they want to enable wifi.
                        return getWifiSetupMessageData();
                    }
                }

                LOGD(TAG, "Simple cards");
                List<ConfMessageCard> simpleCards
                        = ConfMessageCard.getActiveSimpleCards(context);
                if (!simpleCards.isEmpty()) {
                    return getSimpleMessageCardData(simpleCards.get(0));
                }
            }
        }
        return null;
    }

    /**
     * Check if this card should be shown and hasn't previously been dismissed.
     *
     * @return {@code true} if the given message card should be displayed.
     */
    private static boolean shouldShowCard(@NonNull Context context,
            @NonNull ConfMessageCardUtils.ConfMessageCard card) {
        return ConfMessageCardUtils.shouldShowConfMessageCard(context, card) &&
                !ConfMessageCardUtils.hasDismissedConfMessageCard(context, card);
    }
}
