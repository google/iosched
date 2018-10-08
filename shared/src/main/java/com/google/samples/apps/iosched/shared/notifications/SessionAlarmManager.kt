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

import android.app.AlarmManager
import android.app.AlarmManager.RTC
import android.app.AlarmManager.RTC_WAKEUP
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.util.toEpochMilli
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Manages setting and cancelling alarms for sessions.
 */
open class SessionAlarmManager @Inject constructor(val context: Context) {

    private val systemAlarmManager: AlarmManager? = context.getSystemService()

    /**
     * Schedules an alarm for a session.
     */
    fun setAlarmForSession(session: Session) {
        if ((session.startTime.toInstant().minusMillis(alarmTimeDelta)).isBefore(Instant.now())) {
            Timber.d("Trying to schedule an alarm for a past session, ignoring.")
            return
        }
        cancelAlarmForSession(session.id)
        val pendingIntent = makePendingIntent(session.id) ?: return
        scheduleAlarmFor(pendingIntent, session)
    }

    open fun cancelAlarmForSession(sessionId: SessionId) {
        val pendingIntent = makePendingIntent(sessionId) ?: return
        cancelAlarmFor(pendingIntent)
        Timber.d("Cancelled alarm for session $sessionId")
    }

    private fun makePendingIntent(sessionId: SessionId): PendingIntent? {
        val intent = Intent(context, AlarmBroadcastReceiver::class.java)
        intent.putExtra(
            AlarmBroadcastReceiver.SESSION_ID_EXTRA, sessionId)
        return PendingIntent.getBroadcast(
            context, sessionId.hashCode(), intent, FLAG_UPDATE_CURRENT
        )
    }

    private fun cancelAlarmFor(pendingIntent: PendingIntent) {
        try {
            systemAlarmManager?.cancel(pendingIntent)
        } catch (ex: Exception) {
            Timber.e("Couldn't cancel alarm for session")
        }
    }

    private fun scheduleAlarmFor(pendingIntent: PendingIntent, session: Session) {
        systemAlarmManager?.let {
            val triggerAtMillis = session.startTime.toEpochMilli() - alarmTimeDelta
            systemAlarmManager.setExact(RTC_WAKEUP, triggerAtMillis, pendingIntent)
            Timber.d("Scheduled alarm for session ${session.title} at $triggerAtMillis")
        }
    }

    fun dismissNotificationInFiveMinutes(notificationId: Int) {
        systemAlarmManager?.let {
            val intent = Intent(context, CancelNotificationBroadcastReceiver::class.java)
            intent.putExtra(
                CancelNotificationBroadcastReceiver.NOTIFICATION_ID_EXTRA, notificationId)
            val pendingIntent = PendingIntent.getBroadcast(context, notificationId, intent, 0)
            val triggerAtMillis = Instant.now().plus(5, ChronoUnit.MINUTES).toEpochMilli()
            systemAlarmManager.set(RTC, triggerAtMillis, pendingIntent)
            Timber.d("Scheduled notification dismissal for $notificationId at $triggerAtMillis")
        }
    }

    companion object {
        private val alarmTimeDelta = TimeUnit.MINUTES.toMillis(5)
    }
}
