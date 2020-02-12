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

package com.google.samples.apps.iosched.ui.agenda

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.map
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.threeten.bp.ZoneId

class AgendaViewModel @Inject constructor(
    private val loadAgendaUseCase: LoadAgendaUseCase,
    private val getTimeZoneUseCase: GetTimeZoneUseCase
) : ViewModel() {

    private val _agenda = MutableLiveData<List<Block>>()
    val agenda: LiveData<List<Block>> = _agenda

    private val preferConferenceTimeZoneResult = MutableLiveData<Boolean>()
    val timeZoneId = preferConferenceTimeZoneResult.map { inConferenceTimeZone ->
        if (inConferenceTimeZone) {
            TimeUtils.CONFERENCE_TIMEZONE
        } else {
            ZoneId.systemDefault()
        }
    }

    init {
        viewModelScope.launch {
            refreshConferenceTimeZone()
            _agenda.value = loadAgendaUseCase(false).data
        }
    }

    fun refreshAgenda() {
        // Agenda is lightweight and it's not possible to observe the changes with Remote Config,
        // we refresh the agenda on fragment start
        viewModelScope.launch {
            _agenda.value = loadAgendaUseCase(true).data
        }
    }

    private suspend fun refreshConferenceTimeZone() {
        preferConferenceTimeZoneResult.value = getTimeZoneUseCase(Unit).data ?: true
    }
}
