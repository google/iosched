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

package com.google.samples.apps.iosched.shared.domain.login

import android.arch.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.google.samples.apps.iosched.shared.data.login.LoginRepository
import com.google.samples.apps.iosched.shared.domain.ObservableUseCase
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject

/**
 * Gives you a MutableLiveData which will emit the current firebase user whenever it changes.
 */
open class ObservableFirebaseUserUseCase @Inject constructor(
    private val repository: LoginRepository
) : ObservableUseCase<Unit, FirebaseUser?>() {

    override fun execute(parameters: Unit, result: MutableLiveData<Result<FirebaseUser?>>) {
        repository.observableCurrentUser(liveData = result)
    }
}
