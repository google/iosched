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

package com.google.samples.apps.iosched.shared.data.agenda

import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.data.config.AppConfigDataSource

/**
 * Single point of access to agenda data for the presentation layer.
 */
interface AgendaRepository {

    /**
     * Returns a list of [Block]s. When the parameter is passed as true,
     * it's guaranteed the data loaded from this use case is up to date with the
     * remote data source (Remote Config)
     */
    suspend fun getAgenda(forceRefresh: Boolean): List<Block>
}

class DefaultAgendaRepository(
    private val appConfigDataSource: AppConfigDataSource
) : AgendaRepository {

    override suspend fun getAgenda(forceRefresh: Boolean): List<Block> {
        if (forceRefresh) {
            appConfigDataSource.syncStrings()
        }
        return generateBlocks(appConfigDataSource)
    }
}
