package com.google.samples.apps.iosched.explore;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.Nullable;
import android.view.View;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.data.MessageData;
import com.google.samples.apps.iosched.messaging.MessagingRegistration;
import com.google.samples.apps.iosched.messaging.MessagingRegistrationWithGCM;
import com.google.samples.apps.iosched.settings.ConfMessageCardUtils;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.WiFiUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Helper class to create message data view objects representing MessageCards for the Explore I/O
 * stream.
 */
public class MessageCardHelper {
    private static final String TAG = makeLogTag(MessageCardHelper.class);

    private static final String TWITTER_PACKAGE_NAME = "com.twitter.android";
    private static final String GPLUS_PACKAGE_NAME = "com.google.android.apps.plus";

    public static MessageData getSimpleMessageCardData(
      final ConfMessageCardUtils.ConfMessageCard card) {
        MessageData messageData = new MessageData();
        messageData.setEndButtonStringResourceId(R.string.ok);
        messageData.setMessage(card.getSimpleMessage());
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
        messageData.setStartButtonStringResourceId(R.string.explore_io_msgcards_answer_no);
        messageData.setMessageStringResourceId(R.string.explore_io_msgcards_ask_opt_in);
        messageData.setEndButtonStringResourceId(R.string.explore_io_msgcards_answer_yes);

        messageData.setStartButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking conference messages question answered with decline.");
                ConfMessageCardUtils.markAnsweredConfMessageCardsPrompt(view.getContext(), true);
                ConfMessageCardUtils.setConfMessageCardsEnabled(view.getContext(), false);
                Activity activity;
                if ((activity = getActivity(view)) != null) {
                    // This will activate re-registering with the correct GCM topic(s).
                    new MessagingRegistrationWithGCM(activity).registerDevice();
                }
            }
        });
        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking conference messages question answered with affirmation.");
                ConfMessageCardUtils.markAnsweredConfMessageCardsPrompt(view.getContext(), true);
                ConfMessageCardUtils.setConfMessageCardsEnabled(view.getContext(), true);
                Activity activity;
                if ((activity = getActivity(view)) != null) {
                    // This will activate re-registering with the correct GCM topic(s).
                    new MessagingRegistrationWithGCM(activity).registerDevice();
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
        messageData.setStartButtonStringResourceId(R.string.explore_io_msgcards_answer_no);
        messageData.setMessageStringResourceId(R.string.explore_io_notifications_ask_opt_in);
        messageData.setEndButtonStringResourceId(R.string.explore_io_msgcards_answer_yes);

        messageData.setStartButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking notifications question answered with decline.");
                ConfMessageCardUtils.setDismissedConfMessageCard(view.getContext(),
                        ConfMessageCardUtils.ConfMessageCard.SESSION_NOTIFICATIONS, false);
                SettingsUtils.setShowSessionReminders(view.getContext(), false);
                SettingsUtils.setShowSessionFeedbackReminders(view.getContext(), false);
            }
        });
        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking notifications messages question answered with affirmation.");
                ConfMessageCardUtils.setDismissedConfMessageCard(view.getContext(),
                        ConfMessageCardUtils.ConfMessageCard.SESSION_NOTIFICATIONS, true);
                SettingsUtils.setShowSessionReminders(view.getContext(), true);
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
        messageData.setStartButtonStringResourceId(R.string.explore_io_msgcards_answer_no);
        messageData.setMessageStringResourceId(R.string.question_setup_wifi_card_text);
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
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }
}
