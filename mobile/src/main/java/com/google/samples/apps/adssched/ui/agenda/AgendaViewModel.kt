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

package com.google.samples.apps.adssched.ui.agenda

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.adssched.model.Block
import com.google.samples.apps.adssched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.adssched.shared.domain.invoke
import com.google.samples.apps.adssched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.adssched.shared.result.Result
import com.google.samples.apps.adssched.shared.util.TimeUtils
import com.google.samples.apps.adssched.shared.util.map
import org.threeten.bp.ZoneId
import javax.inject.Inject

class AgendaViewModel @Inject constructor(
    loadAgendaUseCase: LoadAgendaUseCase,
    private val getTimeZoneUseCase: GetTimeZoneUseCase
): ViewModel() {

    private val loadAgendaResult = MutableLiveData<Result<List<Block>>>()
    val agenda: LiveData<List<Block>>

    private val preferConferenceTimeZoneResult = MutableLiveData<Result<Boolean>>()
    val timeZoneId: LiveData<ZoneId>

    init {
        agenda = loadAgendaResult.map {
            (it as? Result.Success)?.data ?: emptyList()
        }

        val showInConferenceTimeZone = preferConferenceTimeZoneResult.map {
            (it as? Result.Success<Boolean>)?.data ?: true
        }
        timeZoneId = showInConferenceTimeZone.map { inConferenceTimeZone ->
            if (inConferenceTimeZone) {
                TimeUtils.CONFERENCE_TIMEZONE
            } else {
                ZoneId.systemDefault()
            }
        }

        // Load blocks.
        loadAgendaUseCase(loadAgendaResult)
    }

    fun initializeTimeZone() {
        getTimeZoneUseCase(Unit, preferConferenceTimeZoneResult)
    }
}
