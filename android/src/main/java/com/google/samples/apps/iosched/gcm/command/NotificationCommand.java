/*
 * Copyright 2014 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.gcm.command;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.gcm.GCMCommand;
import com.google.samples.apps.iosched.ui.MyScheduleActivity;
import com.google.samples.apps.iosched.util.PrefUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.gson.Gson;

import java.util.Date;

import static com.google.samples.apps.iosched.util.LogUtils.*;

public class NotificationCommand extends GCMCommand {
    private static final String TAG = makeLogTag("NotificationCommand");

    private static class NotificationCommandModel {
        String format;
        String audience;
        String minVersion;
        String maxVersion;
        String title;
        String message;
        String expiry;
        String dialogTitle;
        String dialogText;
        String dialogYes;
        String dialogNo;
        String url;
    }

    @Override
    public void execute(Context context, String type, String payload) {
        LOGI(TAG, "Received GCM message: " + type);
        LOGI(TAG, "Parsing GCM notification command: " + payload);
        Gson gson = new Gson();
        NotificationCommandModel command;
        try {
            command = gson.fromJson(payload, NotificationCommandModel.class);
            if (command == null) {
                LOGE(TAG, "Failed to parse command (gson returned null).");
                return;
            }
            LOGD(TAG, "Format: " + command.format);
            LOGD(TAG, "Audience: " + command.audience);
            LOGD(TAG, "Title: " + command.title);
            LOGD(TAG, "Message: " + command.message);
            LOGD(TAG, "Expiry: " + command.expiry);
            LOGD(TAG, "URL: " + command.url);
            LOGD(TAG, "Dialog title: " + command.dialogTitle);
            LOGD(TAG, "Dialog text: " + command.dialogText);
            LOGD(TAG, "Dialog yes: " + command.dialogYes);
            LOGD(TAG, "Dialog no: " + command.dialogNo);
            LOGD(TAG, "Min version code: " + command.minVersion);
            LOGD(TAG, "Max version code: " + command.maxVersion);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOGE(TAG, "Failed to parse GCM notification command.");
            return;
        }

        LOGD(TAG, "Processing notification command.");
        processCommand(context, command);
    }

    private void processCommand(Context context, NotificationCommandModel command) {
        // Check format
        if (!"1.0.00".equals(command.format)) {
            LOGW(TAG, "GCM notification command has unrecognized format: " + command.format);
            return;
        }

        // Check app version
        if (!TextUtils.isEmpty(command.minVersion) || !TextUtils.isEmpty(command.maxVersion)) {
            LOGD(TAG, "Command has version range.");
            int minVersion = 0;
            int maxVersion = Integer.MAX_VALUE;
            try {
                if (!TextUtils.isEmpty(command.minVersion)) {
                    minVersion = Integer.parseInt(command.minVersion);
                }
                if (!TextUtils.isEmpty(command.maxVersion)) {
                    maxVersion = Integer.parseInt(command.maxVersion);
                }
                LOGD(TAG, "Version range: " + minVersion + " - " + maxVersion);
                PackageInfo pinfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                LOGD(TAG, "My version code: " + pinfo.versionCode);
                if (pinfo.versionCode < minVersion) {
                    LOGD(TAG, "Skipping command because our version is too old, "
                            + pinfo.versionCode + " < " + minVersion);
                    return;
                }
                if (pinfo.versionCode > maxVersion) {
                    LOGD(TAG, "Skipping command because our version is too new, "
                            + pinfo.versionCode + " > " + maxVersion);
                    return;
                }
            } catch (NumberFormatException ex) {
                LOGE(TAG, "Version spec badly formatted: min=" + command.minVersion
                        + ", max=" + command.maxVersion);
                return;
            } catch (Exception ex) {
                LOGE(TAG, "Unexpected problem doing version check.", ex);
                return;
            }
        }

        // Check if we are the right audience
        LOGD(TAG, "Checking audience: " + command.audience);
        if ("remote".equals(command.audience)) {
            if (PrefUtils.isAttendeeAtVenue(context)) {
                LOGD(TAG, "Ignoring notification because audience is remote and attendee is on-site");
                return;
            } else {
                LOGD(TAG, "Relevant (attendee is remote).");
            }
        } else if ("local".equals(command.audience)) {
            if (!PrefUtils.isAttendeeAtVenue(context)) {
                LOGD(TAG, "Ignoring notification because audience is on-site and attendee is remote.");
                return;
            } else {
                LOGD(TAG, "Relevant (attendee is local).");
            }
        } else if ("all".equals(command.audience)) {
            LOGD(TAG, "Relevant (audience is 'all').");
        } else {
            LOGE(TAG, "Invalid audience on GCM notification command: " + command.audience);
            return;
        }

        // Check if it expired
        Date expiry = command.expiry == null ? null : TimeUtils.parseTimestamp(command.expiry);
        if (expiry == null) {
            LOGW(TAG, "Failed to parse expiry field of GCM notification command: " + command.expiry);
            return;
        } else if (expiry.getTime() < UIUtils.getCurrentTime(context)) {
            LOGW(TAG, "Got expired GCM notification command. Expiry: " + expiry.toString());
            return;
        } else {
            LOGD(TAG, "Message is still valid (expiry is in the future: " + expiry.toString() + ")");
        }

        // decide the intent that will be fired when the user clicks the notification
        Intent intent;
        if (TextUtils.isEmpty(command.dialogText)) {
            // notification leads directly to the URL, no dialog
            if (TextUtils.isEmpty(command.url)) {
                intent = new Intent(context, MyScheduleActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
            } else {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(command.url));
            }
        }  else {
            // use a dialog
            intent = new Intent(context, MyScheduleActivity.class).setFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra(MyScheduleActivity.EXTRA_DIALOG_TITLE,
                    command.dialogTitle == null ? "" : command.dialogTitle);
            intent.putExtra(MyScheduleActivity.EXTRA_DIALOG_MESSAGE,
                    command.dialogText == null ? "" : command.dialogText);
            intent.putExtra(MyScheduleActivity.EXTRA_DIALOG_YES,
                    command.dialogYes == null ? "OK" : command.dialogYes);
            intent.putExtra(MyScheduleActivity.EXTRA_DIALOG_NO,
                    command.dialogNo == null ? "" : command.dialogNo);
            intent.putExtra(MyScheduleActivity.EXTRA_DIALOG_URL,
                    command.url == null ? "" : command.url);
        }

        final String title = TextUtils.isEmpty(command.title) ?
                context.getString(R.string.app_name) : command.title;
        final String message = TextUtils.isEmpty(command.message) ? "" : command.message;

        // fire the notification
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(0, new NotificationCompat.Builder(context)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.ic_stat_notification)
                        .setTicker(command.message)
                        .setContentTitle(title)
                        .setContentText(message)
                        //.setColor(context.getResources().getColor(R.color.theme_primary))
                            // Note: setColor() is available in the support lib v21+.
                            // We commented it out because we want the source to compile 
                            // against support lib v20. If you are using support lib
                            // v21 or above on Android L, uncomment this line.
                        .setContentIntent(PendingIntent.getActivity(context, 0, intent,
                                PendingIntent.FLAG_CANCEL_CURRENT))
                        .setAutoCancel(true)
                        .build());
    }

}
