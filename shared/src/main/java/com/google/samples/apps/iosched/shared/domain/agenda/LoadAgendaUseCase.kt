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

package com.google.samples.apps.iosched.shared.domain.agenda

import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.data.agenda.AgendaRepository
import com.google.samples.apps.iosched.shared.domain.MediatorUseCase
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject

open class LoadAgendaUseCase @Inject constructor(
    private val repository: AgendaRepository
) : MediatorUseCase<Unit, List<Block>>() {

    override fun execute(parameters: Unit) {
        try {
            val observableAgenda = repository.getObservableAgenda()
            result.removeSource(observableAgenda)
            result.addSource(observableAgenda) {
                result.postValue(Result.Success(it))
            }
        } catch (e: Exception) {
            result.postValue(Result.Error(e))
        }
    }
}
