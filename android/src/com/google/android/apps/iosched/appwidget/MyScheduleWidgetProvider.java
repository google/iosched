/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.appwidget;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.HomeActivity;
import com.google.android.apps.iosched.ui.SessionLivestreamActivity;
import com.google.android.apps.iosched.util.AccountUtils;
import com.google.android.apps.iosched.util.ParserUtils;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;

import android.accounts.Account;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.RemoteViews;

import static com.google.android.apps.iosched.util.LogUtils.LOGV;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * The app widget's AppWidgetProvider.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MyScheduleWidgetProvider extends AppWidgetProvider {

    private static final String TAG = makeLogTag(MyScheduleWidgetProvider.class);

    public static final String EXTRA_BLOCK_ID = "block_id";
    public static final String EXTRA_STARRED_SESSION_ID = "star_session_id";
    public static final String EXTRA_BLOCK_TYPE = "block_type";
    public static final String EXTRA_STARRED_SESSION = "starred_session";
    public static final String EXTRA_NUM_STARRED_SESSIONS = "num_starred_sessions";
    public static final String EXTRA_BUTTON = "extra_button";

    public static final String CLICK_ACTION =
            "com.google.android.apps.iosched.appwidget.action.CLICK";
    public static final String REFRESH_ACTION =
            "com.google.android.apps.iosched.appwidget.action.REFRESH";
    public static final String EXTRA_PERFORM_SYNC =
            "com.google.android.apps.iosched.appwidget.extra.PERFORM_SYNC";

    private static Handler sWorkerQueue;

    // TODO: this will get destroyed when the app process is killed. need better observer strategy.
    private static SessionDataProviderObserver sDataObserver;

    public MyScheduleWidgetProvider() {
        // Start the worker thread
        HandlerThread sWorkerThread = new HandlerThread("MyScheduleWidgetProvider-worker");
        sWorkerThread.start();
        sWorkerQueue = new Handler(sWorkerThread.getLooper());
    }

    @Override
    public void onEnabled(Context context) {
        final ContentResolver r = context.getContentResolver();
        if (sDataObserver == null) {
            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            final ComponentName cn = new ComponentName(context, MyScheduleWidgetProvider.class);
            sDataObserver = new SessionDataProviderObserver(mgr, cn, sWorkerQueue);
            r.registerContentObserver(ScheduleContract.Sessions.CONTENT_URI, true, sDataObserver);
        }
    }

    @Override
    public void onReceive(final Context context, Intent widgetIntent) {
        final String action = widgetIntent.getAction();

        if (REFRESH_ACTION.equals(action)) {
            final boolean shouldSync = widgetIntent.getBooleanExtra(EXTRA_PERFORM_SYNC, false);
            sWorkerQueue.removeMessages(0);
            sWorkerQueue.post(new Runnable() {
                @Override
                public void run() {
                    // Trigger sync
                    String chosenAccountName = AccountUtils.getChosenAccountName(context);
                    if (shouldSync && chosenAccountName != null) {
                        Bundle extras = new Bundle();
                        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                        ContentResolver.requestSync(
                                new Account(chosenAccountName, GoogleAccountManager.ACCOUNT_TYPE),
                                ScheduleContract.CONTENT_AUTHORITY, extras);
                    }

                    // Update widget
                    final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                    final ComponentName cn = new ComponentName(context,
                            MyScheduleWidgetProvider.class);
                    mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn),
                            R.id.widget_schedule_list);
                }
            });

        } else if (CLICK_ACTION.equals(action)) {
            String blockId = widgetIntent.getStringExtra(EXTRA_BLOCK_ID);
            int numStarredSessions = widgetIntent.getIntExtra(EXTRA_NUM_STARRED_SESSIONS, 0);
            String starredSessionId = widgetIntent.getStringExtra(EXTRA_STARRED_SESSION_ID);
            String blockType = widgetIntent.getStringExtra(EXTRA_BLOCK_TYPE);
            boolean starredSession = widgetIntent.getBooleanExtra(EXTRA_STARRED_SESSION, false);
            boolean extraButton = widgetIntent.getBooleanExtra(EXTRA_BUTTON, false);

            LOGV(TAG, "blockId:" + blockId);
            LOGV(TAG, "starredSession:" + starredSession);
            LOGV(TAG, "blockType:" + blockType);
            LOGV(TAG, "numStarredSessions:" + numStarredSessions);
            LOGV(TAG, "extraButton:" + extraButton);

            if (ParserUtils.BLOCK_TYPE_SESSION.equals(blockType)
                    || ParserUtils.BLOCK_TYPE_CODE_LAB.equals(blockType)) {
                Uri sessionsUri;
                if (numStarredSessions == 0) {
                    sessionsUri = ScheduleContract.Blocks.buildSessionsUri(
                            blockId);
                    LOGV(TAG, "sessionsUri:" + sessionsUri);
                    Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else if (numStarredSessions == 1) {
                    if (extraButton) {
                        sessionsUri = ScheduleContract.Blocks.buildSessionsUri(blockId);
                        LOGV(TAG, "sessionsUri extraButton:" + sessionsUri);
                        Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } else {
                        sessionsUri = ScheduleContract.Sessions.buildSessionUri(starredSessionId);
                        LOGV(TAG, "sessionsUri:" + sessionsUri);
                        Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                } else {
                    if (extraButton) {
                        sessionsUri = ScheduleContract.Blocks.buildSessionsUri(blockId);
                        LOGV(TAG, "sessionsUri extraButton:" + sessionsUri);
                        Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } else {
                        sessionsUri = ScheduleContract.Blocks.buildStarredSessionsUri(blockId);
                        LOGV(TAG, "sessionsUri:" + sessionsUri);
                        Intent intent = new Intent(Intent.ACTION_VIEW, sessionsUri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                }
            } else if (ParserUtils.BLOCK_TYPE_KEYNOTE.equals(blockType)) {
                Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(starredSessionId);
                LOGV(TAG, "sessionUri:" + sessionUri);
                Intent intent = new Intent(Intent.ACTION_VIEW, sessionUri);
                intent.setClass(context, SessionLivestreamActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
        super.onReceive(context, widgetIntent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final boolean isAuthenticated = AccountUtils.isAuthenticated(context);
        for (int appWidgetId : appWidgetIds) {
            // Specify the service to provide data for the collection widget.  Note that we need to
            // embed the appWidgetId via the data otherwise it will be ignored.
            final Intent intent = new Intent(context, MyScheduleWidgetService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            final RemoteViews rv = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);

            compatSetRemoteAdapter(rv, appWidgetId, intent);

            // Set the empty view to be displayed if the collection is empty.  It must be a sibling
            // view of the collection view.
            rv.setEmptyView(R.id.widget_schedule_list, android.R.id.empty);
            rv.setTextViewText(android.R.id.empty, context.getResources().getString(isAuthenticated
                    ? R.string.empty_widget_text
                    : R.string.empty_widget_text_signed_out));

            final Intent onClickIntent = new Intent(context, MyScheduleWidgetProvider.class);
            onClickIntent.setAction(MyScheduleWidgetProvider.CLICK_ACTION);
            onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
            final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0,
                    onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.widget_schedule_list, onClickPendingIntent);

            final Intent refreshIntent = new Intent(context, MyScheduleWidgetProvider.class);
            refreshIntent.setAction(MyScheduleWidgetProvider.REFRESH_ACTION);
            refreshIntent.putExtra(MyScheduleWidgetProvider.EXTRA_PERFORM_SYNC, true);
            final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0,
                    refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent);

            final Intent openAppIntent = new Intent(context, HomeActivity.class);
            final PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0,
                    openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.widget_logo, openAppPendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void compatSetRemoteAdapter(RemoteViews rv, int appWidgetId, Intent intent) {
        if (UIUtils.hasICS()) {
            rv.setRemoteAdapter(R.id.widget_schedule_list, intent);
        } else {
            //noinspection deprecation
            rv.setRemoteAdapter(appWidgetId, R.id.widget_schedule_list, intent);
        }
    }

    private static class SessionDataProviderObserver extends ContentObserver {
        private AppWidgetManager mAppWidgetManager;
        private ComponentName mComponentName;

        SessionDataProviderObserver(AppWidgetManager appWidgetManager, ComponentName componentName,
                Handler handler) {
            super(handler);
            mAppWidgetManager = appWidgetManager;
            mComponentName = componentName;
        }

        @Override
        public void onChange(boolean selfChange) {
            // The data has changed, so notify the widget that the collection view needs to be updated.
            // In response, the factory's onDataSetChanged() will be called which will requery the
            // cursor for the new data.
            mAppWidgetManager.notifyAppWidgetViewDataChanged(
                    mAppWidgetManager.getAppWidgetIds(mComponentName),
                    R.id.widget_schedule_list);
        }
    }
}
