/*
 * Copyright 2020 Google LLC
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

package com.google.samples.apps.iosched.shared.domain.feed

import com.google.samples.apps.iosched.shared.di.MainDispatcher
import com.google.samples.apps.iosched.shared.domain.FlowUseCase
import com.google.samples.apps.iosched.shared.domain.feed.ConferenceState.ENDED
import com.google.samples.apps.iosched.shared.domain.feed.ConferenceState.STARTED
import com.google.samples.apps.iosched.shared.domain.feed.ConferenceState.UPCOMING
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.TimeUtils
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import org.threeten.bp.Duration

enum class ConferenceState { UPCOMING, STARTED, ENDED }

/**
 * Gets the current [ConferenceState].
 */
class GetConferenceStateUseCase @Inject constructor(
    @MainDispatcher val mainDispatcher: CoroutineDispatcher,
    private val timeProvider: TimeProvider
) : FlowUseCase<Unit?, ConferenceState>(mainDispatcher) {

    override fun execute(parameters: Unit?): Flow<Result<ConferenceState>> {
        return moveToNextState().map { Result.Success(it) }
    }

    private fun moveToNextState(): Flow<ConferenceState> = flow {
        do {
            val (nextState, delayForLaterState) = getNextStateWithDelay()
            emit(nextState)
            delay(delayForLaterState ?: 0)
        } while (nextState != ENDED)
    }

    private fun getNextStateWithDelay(): Pair<ConferenceState, Long?> {
        val timeUntilStart = Duration.between(timeProvider.now(), TimeUtils.getKeynoteStartTime())
        if (timeUntilStart.isNegative) {
            val timeUntilEnd =
                Duration.between(timeProvider.now(), TimeUtils.getConferenceEndTime())
            if (timeUntilEnd.isNegative) {
                return Pair(ENDED, null)
            } else {
                return Pair(STARTED, timeUntilEnd.toMillis())
            }
        } else {
            return Pair(UPCOMING, timeUntilStart.toMillis())
        }
    }
}
