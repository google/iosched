/*
 * Copyright 2021 Google LLC
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

package com.google.samples.apps.iosched.ui.schedule

import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.util.tryOffer
import com.google.samples.apps.iosched.ui.sessioncommon.OnSessionClickListener
import com.google.samples.apps.iosched.ui.sessioncommon.OnSessionStarClickDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

// Note: clients should obtain this from the Activity.
@HiltViewModel
class ScheduleTwoPaneViewModel @Inject constructor(
    onSessionStarClickDelegate: OnSessionStarClickDelegate
) : ViewModel(),
    OnSessionClickListener,
    OnSessionStarClickDelegate by onSessionStarClickDelegate {

    private val _isTwoPane = MutableStateFlow(false)
    val isTwoPane: StateFlow<Boolean> = _isTwoPane

    private val _returnToListPaneEvents = Channel<Unit>(capacity = Channel.CONFLATED)
    val returnToListPaneEvents = _returnToListPaneEvents.receiveAsFlow()

    private val _selectSessionEvents = Channel<SessionId>(capacity = Channel.CONFLATED)
    val selectSessionEvents = _selectSessionEvents.receiveAsFlow()

    fun setIsTwoPane(isTwoPane: Boolean) {
        _isTwoPane.value = isTwoPane
    }

    fun returnToListPane() {
        _returnToListPaneEvents.tryOffer(Unit)
    }

    override fun openEventDetail(id: SessionId) {
        _selectSessionEvents.tryOffer(id)
    }
}
