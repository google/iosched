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

package com.google.samples.apps.iosched.shared.domain.sessions

import androidx.lifecycle.LiveData
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.userevent.ObservableUserEvents
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.notifications.SessionAlarmManager
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Sets a notification for each session that is starred or reserved by the user.
 */
@Singleton
class NotificationAlarmUpdater @Inject constructor(
    private val alarmManager: SessionAlarmManager,
    private val repository: SessionAndUserEventRepository
) {
    var observer: ((Result<ObservableUserEvents>) -> Unit)? = null
    var userEvents: LiveData<Result<ObservableUserEvents>>? = null

    var cancelObserver: ((Result<ObservableUserEvents>) -> Unit)? = null
    var cancelUserEvents: LiveData<Result<ObservableUserEvents>>? = null

    fun updateAll(userId: String) {

        // Go through every UserSession and make sure the alarm is set for the notification.

        val newObserver = { sessions: Result<ObservableUserEvents> ->
            when (sessions) {
                is Success -> processEvents(userId, sessions.data)
                is Error -> Timber.e(sessions.cause)
            }
        }
        userEvents = repository.getObservableUserEvents(userId).apply {
            observeForever(newObserver)
        }
        observer = newObserver
    }

    private fun processEvents(
        userId: String,
        sessions: ObservableUserEvents
    ) {
        Timber.d("Setting all the alarms for user $userId")
        val startWork = System.currentTimeMillis()
        sessions.userSessions.forEach { session: UserSession ->
            if (session.userEvent.isStarred || session.userEvent.isReserved()) {
                alarmManager.setAlarmForSession(session)
            }
        }
        Timber.d("Work finished in ${System.currentTimeMillis() - startWork} ms")
        clear()
    }

    fun clear() {
        observer?.let {
            userEvents?.removeObserver(it)
        }
        cancelObserver?.let {
            cancelUserEvents?.removeObserver(it)
        }
        observer = null
        cancelObserver = null
        userEvents = null
        cancelUserEvents = null
    }

    fun cancelAll() {

        val newObserver = { sessions: Result<ObservableUserEvents> ->
            when (sessions) {
                is Success -> DefaultScheduler.execute {
                    cancelAllSessions(sessions.data)
                }
                is Error -> Timber.e(sessions.cause)
            }
        }
        repository.getObservableUserEvents(null).observeForever(newObserver)
        cancelObserver = newObserver
        clear()
    }

    private fun cancelAllSessions(sessions: ObservableUserEvents) {
        Timber.d("Cancelling all the alarms")
        sessions.userSessions.forEach {
            alarmManager.cancelAlarmForSession(it.session.id)
        }
    }
}

@Singleton
open class StarReserveNotificationAlarmUpdater @Inject constructor(
    private val alarmManager: SessionAlarmManager
) {
    open fun updateSession(
        userSession: UserSession,
        requestNotification: Boolean
    ) {
        if (requestNotification) {
            alarmManager.setAlarmForSession(userSession)
        } else {
            alarmManager.cancelAlarmForSession(userSession.session.id)
        }
    }
}
