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

package com.google.samples.apps.iosched.appwidget;

import android.accounts.Account;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.ui.MyScheduleActivity;
import com.google.samples.apps.iosched.ui.TaskStackBuilderProxyActivity;
import com.google.samples.apps.iosched.util.AccountUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * The app widget's AppWidgetProvider.
 */
public class ScheduleWidgetProvider extends AppWidgetProvider {
    private static final String TAG = makeLogTag(ScheduleWidgetProvider.class);

    private static final String REFRESH_ACTION =
            "com.google.samples.apps.iosched.appwidget.action.REFRESH";
    private static final String EXTRA_PERFORM_SYNC =
            "com.google.samples.apps.iosched.appwidget.extra.PERFORM_SYNC";

    public static Intent getRefreshBroadcastIntent(Context context, boolean performSync) {
        return new Intent(REFRESH_ACTION)
                .setComponent(new ComponentName(context, ScheduleWidgetProvider.class))
                .putExtra(EXTRA_PERFORM_SYNC, performSync);
    }

    @Override
    public void onReceive(final Context context, Intent widgetIntent) {
        final String action = widgetIntent.getAction();

        if (REFRESH_ACTION.equals(action)) {
            LOGD(TAG, "received REFRESH_ACTION from widget");
            final boolean shouldSync = widgetIntent.getBooleanExtra(EXTRA_PERFORM_SYNC, false);

            // Trigger sync
            Account chosenAccount = AccountUtils.getActiveAccount(context);
            if (shouldSync && chosenAccount != null) {
                SyncHelper.requestManualSync(chosenAccount);
            }

            // Notify the widget that the list view needs to be updated.
            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final ComponentName cn = new ComponentName(context, ScheduleWidgetProvider.class);
            mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn),
                    R.id.widget_schedule_list);

        }
        super.onReceive(context, widgetIntent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        LOGD(TAG, "updating app widget");
        final boolean isAuthenticated = AccountUtils.hasActiveAccount(context);
        for (int appWidgetId : appWidgetIds) {
            // Specify the service to provide data for the collection widget.  Note that we need to
            // embed the appWidgetId via the data otherwise it will be ignored.
            final Intent intent = new Intent(context, ScheduleWidgetRemoteViewsService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget);
            rv.setRemoteAdapter(R.id.widget_schedule_list, intent);

            // Set the empty view to be displayed if the collection is empty.  It must be a sibling
            // view of the collection view.
            rv.setEmptyView(R.id.widget_schedule_list, android.R.id.empty);
            LOGD(TAG, "setting widget empty view");
            rv.setTextViewText(android.R.id.empty, context.getResources().getString(isAuthenticated
                    ? R.string.empty_widget_text
                    : R.string.empty_widget_text_signed_out));

            final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0,
                    getRefreshBroadcastIntent(context, true), PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent);

            final Intent onClickIntent = TaskStackBuilderProxyActivity.getTemplate(context);
            final PendingIntent onClickPendingIntent = PendingIntent.getActivity(context, 0,
                    onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.widget_schedule_list, onClickPendingIntent);

            final Intent openAppIntent = new Intent(context, MyScheduleActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            final PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0,
                    openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widget_logo, openAppPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
