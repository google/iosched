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
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.notifications.SessionAlarmManager
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets a notification for each session that is starred by the user.
 */
@Singleton
class NotificationAlarmUpdater @Inject constructor(
    private val alarmManager: SessionAlarmManager,
    private val repository: SessionAndUserEventRepository
) {
    var observer: ((Result<LoadUserSessionsByDayUseCaseResult>) -> Unit)? = null
    var userEvents: LiveData<Result<LoadUserSessionsByDayUseCaseResult>>? = null

    var cancelObserver: ((Result<LoadUserSessionsByDayUseCaseResult>) -> Unit)? = null
    var cancelUserEvents: LiveData<Result<LoadUserSessionsByDayUseCaseResult>>? = null

    fun updateAll(userId: String) {

        // Go through every UserSession and make sure the alarm is set for the notification.

        val newObserver = { sessions: Result<LoadUserSessionsByDayUseCaseResult> ->
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
        sessions: LoadUserSessionsByDayUseCaseResult
    ) {
        Timber.d("Setting all the alarms for user $userId")
        val startWork = System.currentTimeMillis()
        sessions.userSessionsPerDay.forEach { day ->
            day.value.forEach { it: UserSession ->
                if (it.userEvent.isStarred) {
                    alarmManager.setAlarmForSession(it.session)
                }
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

        val newObserver = { sessions: Result<LoadUserSessionsByDayUseCaseResult> ->
            when (sessions) {
                is Success -> cancelAllSessions(sessions.data)
                is Error -> Timber.e(sessions.cause)
            }
        }
        repository.getObservableUserEvents(null).observeForever(newObserver)
        cancelObserver = newObserver
        clear()
    }

    private fun cancelAllSessions(sessions: LoadUserSessionsByDayUseCaseResult) {
        Timber.d("Cancelling all the alarms")
        sessions.userSessionsPerDay.forEach { day ->
            day.value.forEach { it: UserSession ->
                alarmManager.cancelAlarmForSession(it.session.id)
            }
        }
    }
}

@Singleton
open class StarNotificationAlarmUpdater @Inject constructor(
    private val alarmManager: SessionAlarmManager

) {
    open fun updateSession(session: Session, starred: Boolean) {
        if (starred) {
            alarmManager.setAlarmForSession(session)
        } else {
            alarmManager.cancelAlarmForSession(session.id)
        }
    }
}
