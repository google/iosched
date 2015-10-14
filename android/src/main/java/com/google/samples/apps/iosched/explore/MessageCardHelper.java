package com.google.samples.apps.iosched.explore;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.data.MessageData;
import com.google.samples.apps.iosched.settings.ConfMessageCardUtils;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.WiFiUtils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;

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

    /**
     * Return the conference messages opt-in data.
     */
    public static MessageData getConferenceOptInMessageData(Context context) {
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

            }
        });
        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking conference messages question answered with affirmation.");
                ConfMessageCardUtils.markAnsweredConfMessageCardsPrompt(view.getContext(), true);
                ConfMessageCardUtils.setConfMessageCardsEnabled(view.getContext(), true);
            }
        });

        return messageData;
    }

    /**
     * Return the wifi setup card data.
     */
    public static MessageData getWifiSetupMessageData(Context context) {
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
     * Return card data representing a message to send to users before registering.
     */
    public static MessageData getConferenceCredentialsMessageData(Context context) {
        MessageData messageData = new MessageData();
        messageData.setMessageStringResourceId(R.string.explore_io_msgcards_conf_creds_card);
        messageData.setEndButtonStringResourceId(R.string.got_it);
        messageData.setIconDrawableId(R.drawable.message_card_credentials);

        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking conference credentials card dismissed.");

                ConfMessageCardUtils.markDismissedConfMessageCard(
                        view.getContext(),
                        ConfMessageCardUtils.ConfMessageCard.CONFERENCE_CREDENTIALS);
            }
        });

        return messageData;
    }

    /**
     * Return card data representing a message to allow attendees to provide wifi feedback.
     */
    public static MessageData getWifiFeedbackMessageData(Context context) {
        MessageData messageData = new MessageData();
        messageData.setMessageStringResourceId(R.string.explore_io_msgcards_wifi_feedback);
        messageData.setStartButtonStringResourceId(R.string.explore_io_msgcards_answer_no);
        messageData.setEndButtonStringResourceId(R.string.explore_io_msgcards_answer_yes);
        messageData.setIconDrawableId(R.drawable.message_card_wifi);

        messageData.setStartButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking conference credentials card dismissed.");
                ConfMessageCardUtils.markDismissedConfMessageCard(
                        view.getContext(),
                        ConfMessageCardUtils.ConfMessageCard.WIFI_FEEDBACK);
            }
        });

        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Providing feedback");
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "#io15wifi ");
                sendIntent.setType("text/plain");

                boolean isGPlusInstalled = isPackageInstalledAndEnabled(view.getContext(),
                        GPLUS_PACKAGE_NAME);
                boolean isTwitterInstalled = isPackageInstalledAndEnabled(view.getContext(),
                        TWITTER_PACKAGE_NAME);

                if (isGPlusInstalled) {
                    sendIntent.setPackage(GPLUS_PACKAGE_NAME);
                } else if (isTwitterInstalled) {
                    sendIntent.setPackage(TWITTER_PACKAGE_NAME);
                }

                view.getContext().startActivity(sendIntent);
                // Hide the card for now.
                ConfMessageCardUtils.markShouldShowConfMessageCard(view.getContext(),
                        ConfMessageCardUtils.ConfMessageCard.WIFI_FEEDBACK, false);
            }
        });


        return messageData;
    }

    /**
     * Return card data for instructions on where to queue for the Keynote.
     */
    public static MessageData getKeynoteAccessMessageData(Context context) {
        MessageData messageData = new MessageData();
        messageData.setMessageStringResourceId(R.string.explore_io_msgcards_keynote_access_card);
        messageData.setEndButtonStringResourceId(R.string.got_it);
        messageData.setIconDrawableId(R.drawable.message_card_keynote);

        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking keynote access card dismissed.");

                ConfMessageCardUtils.markDismissedConfMessageCard(
                        view.getContext(),
                        ConfMessageCardUtils.ConfMessageCard.KEYNOTE_ACCESS);
            }
        });

        return messageData;
    }

    /**
     * Return card data for information about the After Hours party.
     */
    public static MessageData getAfterHoursMessageData(Context context) {
        MessageData messageData = new MessageData();
        messageData.setMessageStringResourceId(R.string.explore_io_msgcards_after_hours_card);
        messageData.setEndButtonStringResourceId(R.string.got_it);
        messageData.setIconDrawableId(R.drawable.message_card_after_hours);

        messageData.setEndButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LOGD(TAG, "Marking after hours card dismissed.");

                ConfMessageCardUtils.markDismissedConfMessageCard(
                        view.getContext(),
                        ConfMessageCardUtils.ConfMessageCard.AFTER_HOURS);
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
}
