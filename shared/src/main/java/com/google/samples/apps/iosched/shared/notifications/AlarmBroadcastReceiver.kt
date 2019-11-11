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

package com.google.samples.apps.iosched.shared.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.R
import com.google.samples.apps.iosched.shared.data.prefs.SharedPreferenceStorage
import com.google.samples.apps.iosched.shared.data.signin.datasources.AuthIdDataSource
import com.google.samples.apps.iosched.shared.domain.sessions.LoadSessionSyncUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionSyncUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import dagger.android.DaggerBroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.threeten.bp.Instant
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Receives broadcast intents with information for session notifications.
 */
class AlarmBroadcastReceiver : DaggerBroadcastReceiver() {

    @Inject
    lateinit var sharedPreferencesStorage: SharedPreferenceStorage

    @Inject
    lateinit var loadUserSession: LoadUserSessionSyncUseCase

    @Inject
    lateinit var loadSession: LoadSessionSyncUseCase

    @Inject
    lateinit var alarmManager: SessionAlarmManager

    @Inject
    lateinit var authIdDataSource: AuthIdDataSource

    // Coroutines scope for AlarmBroadcastReceiver background work
    private val alarmScope: CoroutineScope =
        CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Timber.e("Alarm received")
        val sessionId = intent.getStringExtra(
            SESSION_ID_EXTRA
        ) ?: return
        val userId = authIdDataSource.getUserId()
        if (userId != null) {
            alarmScope.launch {
                checkThenShowNotification(context, sessionId, userId)
            }
        } else {
            Timber.e("No user ID, not showing notification.")
        }
    }

    @WorkerThread
    private suspend fun checkThenShowNotification(
        context: Context,
        sessionId: String,
        userId: String
    ) {
        if (!sharedPreferencesStorage.preferToReceiveNotifications) {
            Timber.d("User disabled notifications, not showing")
            return
        }

        Timber.d("Showing notification for $sessionId, user $userId")

        val userEvent: Result<UserSession>? = getUserEvent(userId, sessionId)
        // Don't notify if for some reason the event is no longer starred.
        if (userEvent is Success) {
            if (userEvent.data.userEvent.isStarred && isSessionCurrent(userEvent.data.session)) {
                try {
                    val notificationId = showNotification(context, userEvent.data.session)
                    // Dismiss in any case
                    alarmManager.dismissNotificationInFiveMinutes(notificationId)
                } catch (ex: Exception) {
                    Timber.e(ex)
                    return
                }
            }
        } else {
            // There was no way to get UserEvent info, notify in case of connectivity error.
            notifyWithoutUserEvent(sessionId, context)
        }
    }

    private suspend fun notifyWithoutUserEvent(sessionId: String, context: Context) {
        // Using supervisorScope to not propagate a possible exception that'd make the app crash
        supervisorScope {
            // Using async coroutine builder to wait for the result of the use case computation
            val session = async { loadSession(sessionId) }.await()
            if (session is Success) {
                val notificationId = showNotification(context, session.data)
                alarmManager.dismissNotificationInFiveMinutes(notificationId)
            } else {
                Timber.e("Session couldn't be loaded for notification")
            }
        }
    }

    private suspend fun getUserEvent(userId: String, sessionId: String): Result<UserSession>? {
        return try {
            // Using coroutineScope to propagate exception to the try/catch block
            coroutineScope {
                // Using async coroutine builder to wait for the result of the use case computation
                async { loadUserSession(userId to sessionId) }.await()
            }
        } catch (ex: Exception) {
            Timber.e(
                "Session notification is set, however there was an error confirming that " +
                    "the event is still starred. Showing notification."
            )
            null
        }
    }

    @WorkerThread
    private fun showNotification(context: Context, session: Session): Int {
        val notificationManager: NotificationManager = context.getSystemService()
            ?: throw Exception("Notification Manager not found.")

        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            makeNotificationChannel(context, notificationManager)
        }

        val intent = Intent(
            ACTION_VIEW,
            "adssched://sessions/${session.id}".toUri()
        )
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        // Create the TaskStackBuilder
        val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(context)
            // Add the intent, which inflates the back stack
            .addNextIntentWithParentStack(intent)
            // Get the PendingIntent containing the entire back stack
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, ALARM_BROADCAST_CHANNEL_ID)
            .setContentTitle(session.title)
            .setContentText(context.getString(R.string.starting_soon))
            .setSmallIcon(R.drawable.ic_notif_session_starting)
            .setContentIntent(resultPendingIntent)
            .setTimeoutAfter(TimeUnit.MINUTES.toMillis(10)) // Backup (cancelled with receiver)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notificationId = session.id.hashCode()
        notificationManager.notify(notificationId, notification)
        return notificationId
    }

    @RequiresApi(VERSION_CODES.O)
    private fun makeNotificationChannel(
        context: Context,
        notificationManager: NotificationManager
    ) {
        val channelId =
            ALARM_BROADCAST_CHANNEL_ID
        val channel = NotificationChannel(
            channelId,
            context.getString(R.string.session_notifications),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { lockscreenVisibility = Notification.VISIBILITY_PUBLIC }

        notificationManager.createNotificationChannel(channel)
    }

    private fun isSessionCurrent(session: Session): Boolean {
        return session.startTime.toInstant().isAfter(Instant.now())
    }

    companion object {
        const val SESSION_ID_EXTRA = "user_event_extra"
        const val QUERY_SESSION_ID = "session_id"
        private const val ALARM_BROADCAST_CHANNEL_ID = "alarm_broadcast_channel_id"
    }
}
