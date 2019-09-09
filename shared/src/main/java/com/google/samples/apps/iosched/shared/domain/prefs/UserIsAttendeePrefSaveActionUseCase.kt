/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.shared.domain.prefs

import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.data.prefs.UserIsAttendee.IN_PERSON
import com.google.samples.apps.iosched.shared.data.prefs.UserIsAttendee.REMOTE
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber
import javax.inject.Inject

/**
 * Records the user preference indicating whether the user is attending the conference.
 */
open class UserIsAttendeePrefSaveActionUseCase @Inject constructor(
    private val preferenceStorage: PreferenceStorage,
    private val conferenceDataRepository: ConferenceDataRepository,
    @IoDispatcher defaultDispatcher: CoroutineDispatcher
) : UseCase<Boolean, Boolean>(defaultDispatcher) {

    override fun execute(parameters: Boolean): Boolean {
        preferenceStorage.userIsAttendee = if (parameters) IN_PERSON else REMOTE
        // Force an update of the schedule
        try {
            conferenceDataRepository.refreshCacheWithRemoteConferenceData()
        } catch (t: Throwable) {
            Timber.e(t)
        }
        return parameters
    }
}
