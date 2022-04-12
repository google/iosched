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

package com.google.samples.apps.iosched.shared.data.ar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.functions.FirebaseFunctions
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject

/**
 * Interface that checkes if a signed user is able to demo the AR feature
 */
interface ArDebugFlagEndpoint {

    /**
     * Asks if the signed in user can demo the AR feature (bypass the teaser page)
     */
    fun canDemoAr(): LiveData<Result<Boolean>>
}

class DefaultArDebugFlagEndpoint @Inject constructor(private val functions: FirebaseFunctions) :
    ArDebugFlagEndpoint {

    override fun canDemoAr(): LiveData<Result<Boolean>> {
        val result = MutableLiveData<Result<Boolean>>()
        functions
            .getHttpsCallable("canDemoAr")
            .call()
            .addOnCompleteListener { task ->
                result.postValue(
                    if (task.isSuccessful) {
                        val taskResult = task.result?.data as Map<*, *>
                        if (taskResult["whitelisted"] == true) {
                            Result.Success(true)
                        } else {
                            Result.Success(false)
                        }
                    } else {
                        Result.Error(RuntimeException(task.exception))
                    }
                )
            }
        return result
    }
}
