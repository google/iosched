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

package com.google.samples.apps.iosched.ui.codelabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.shared.domain.codelabs.GetCodelabsInfoCardShownUseCase
import com.google.samples.apps.iosched.shared.domain.codelabs.LoadCodelabsUseCase
import com.google.samples.apps.iosched.shared.domain.codelabs.SetCodelabsInfoCardShownUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.util.WhileViewSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections.emptyList
import javax.inject.Inject

@HiltViewModel
class CodelabsViewModel @Inject constructor(
    private val loadCodelabsUseCase: LoadCodelabsUseCase,
    private val getCodelabsInfoCardShownUseCase: GetCodelabsInfoCardShownUseCase,
    private val setCodelabsInfoCardShownUseCase: SetCodelabsInfoCardShownUseCase
) : ViewModel() {

    val codelabs: StateFlow<List<Any>> = getCodelabsInfoCardShownUseCase(Unit).map {
        // Refresh codelabs when infoCardShownResult changes.
        refreshCodelabs(it)
    }.stateIn(viewModelScope, WhileViewSubscribed, emptyList())

    private suspend fun refreshCodelabs(cardShown: Result<Boolean>): List<Any> {
        val codelabs = loadCodelabsUseCase(Unit)

        val items = mutableListOf<Any>()
        if (!cardShown.successOr(false)) {
            items.add(CodelabsInformationCard)
        }
        items.add(CodelabsHeaderItem)
        items.addAll(codelabs.successOr(emptyList()))
        return items
    }

    fun dismissCodelabsInfoCard() {
        viewModelScope.launch {
            setCodelabsInfoCardShownUseCase(Unit)
        }
    }
}
