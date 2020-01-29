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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.shared.domain.codelabs.GetCodelabsInfoCardShownUseCase
import com.google.samples.apps.iosched.shared.domain.codelabs.LoadCodelabsUseCase
import com.google.samples.apps.iosched.shared.domain.codelabs.SetCodelabsInfoCardShownUseCase
import com.google.samples.apps.iosched.shared.result.successOr
import javax.inject.Inject
import kotlinx.coroutines.launch

class CodelabsViewModel @Inject constructor(
    private val loadCodelabsUseCase: LoadCodelabsUseCase,
    private val getCodelabsInfoCardShownUseCase: GetCodelabsInfoCardShownUseCase,
    private val setCodelabsInfoCardShownUseCase: SetCodelabsInfoCardShownUseCase
) : ViewModel() {

    private val _codelabs = MutableLiveData<List<Any>>()
    val codelabs: LiveData<List<Any>> = _codelabs

    init {
        viewModelScope.launch {
            refreshCodelabs()
        }
    }

    private suspend fun refreshCodelabs() {
        // Refresh codelabs when infoCardShownResult changes.
        val cardShown = getCodelabsInfoCardShownUseCase(Unit)
        val codelabs = loadCodelabsUseCase(Unit)

        val items = mutableListOf<Any>()
        if (!cardShown.successOr(false)) {
            items.add(CodelabsInformationCard)
        }
        items.add(CodelabsHeaderItem)
        items.addAll(codelabs.successOr(emptyList()))
        _codelabs.value = items
    }

    fun dismissCodelabsInfoCard() {
        viewModelScope.launch {
            setCodelabsInfoCardShownUseCase(Unit)
            refreshCodelabs()
        }
    }
}
