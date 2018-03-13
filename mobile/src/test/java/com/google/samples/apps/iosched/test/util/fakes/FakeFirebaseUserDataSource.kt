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

package com.google.samples.apps.iosched.test.util.fakes

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.google.samples.apps.iosched.shared.data.login.FirebaseUserDataSource
import com.google.samples.apps.iosched.shared.result.Result

const val FAKE_TOKEN = "3141592"

class FakeFirebaseUserDataSource(val mockFirebaseUser: FirebaseUser?) :
        FirebaseUserDataSource {

    override fun getToken(): LiveData<Result<String>> {
        return MutableLiveData<Result<String>>().apply { value = Result.Success(FAKE_TOKEN )}
    }

    override fun getCurrentUser(): LiveData<Result<FirebaseUser?>?> {
        return MutableLiveData<Result<FirebaseUser?>>()
                .apply { value =  Result.Success(mockFirebaseUser) }
    }
}
