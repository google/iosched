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

import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.userevent.ObservableUserEvents
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.di.DefaultDispatcher
import com.google.samples.apps.iosched.shared.notifications.SessionAlarmManager
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.data
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Sets a notification for each session that is starred or reserved by the user.
 */
@Singleton
class NotificationAlarmUpdater @Inject constructor(
    private val alarmManager: SessionAlarmManager,
    private val repository: SessionAndUserEventRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) {

    // Coroutines scope for NotificationAlarmUpdater background work
    private val alarmUpdaterScope: CoroutineScope =
        CoroutineScope(defaultDispatcher + SupervisorJob())

    fun updateAll(userId: String) {
        alarmUpdaterScope.launch {
            val events = repository.getObservableUserEvents(userId).first { it is Result.Success }
            events.data?.let { data ->
                processEvents(userId, data)
            }
        }
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
    }

    fun cancelAll() {
        alarmUpdaterScope.launch {
            val events = repository.getObservableUserEvents(null).first { it is Result.Success }
            events.data?.let { data ->
                cancelAllSessions(data)
            }
        }
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
