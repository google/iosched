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

package com.google.samples.apps.iosched.shared.domain.codelabs

import com.google.samples.apps.iosched.model.Codelab
import com.google.samples.apps.iosched.shared.data.codelabs.CodelabsRepository
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.CoroutinesUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

class LoadCodelabsUseCase @Inject constructor(
    private val repository: CodelabsRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : CoroutinesUseCase<Unit, List<Codelab>>(ioDispatcher) {

    override suspend fun execute(parameters: Unit): List<Codelab> {
        return repository.getCodelabs()
            .sortedWith(compareByDescending<Codelab> { it.sortPriority }.thenBy { it.title })
    }
}
