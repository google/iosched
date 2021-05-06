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

package com.google.samples.apps.iosched.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.shared.domain.prefs.OnboardingCompleteActionUseCase
import com.google.samples.apps.iosched.shared.util.tryOffer
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Records that onboarding has been completed and navigates user onward.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingCompleteActionUseCase: OnboardingCompleteActionUseCase,
    signInViewModelDelegate: SignInViewModelDelegate
) : ViewModel(), SignInViewModelDelegate by signInViewModelDelegate {

    private val _navigationActions = Channel<OnboardingNavigationAction>(Channel.CONFLATED)
    // OnboardingViewModel is a shared ViewModel. Therefore, the navigation actions could be
    // received by multiple collectors at the same time. With `receiveAsFlow`, we make sure only
    // one collector will process the navigation event to avoid multiple back stack entries.
    val navigationActions = _navigationActions.receiveAsFlow()

    fun getStartedClick() {
        viewModelScope.launch {
            onboardingCompleteActionUseCase(true)
            _navigationActions.send(OnboardingNavigationAction.NavigateToMainScreen)
        }
    }

    fun onSigninClicked() {
        _navigationActions.tryOffer(OnboardingNavigationAction.NavigateToSignInDialog)
    }
}

sealed class OnboardingNavigationAction {
    object NavigateToMainScreen : OnboardingNavigationAction()
    object NavigateToSignInDialog : OnboardingNavigationAction()
}
