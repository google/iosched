/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.shared.usecases

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.usecases.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.usecases.internal.Scheduler
import timber.log.Timber

/**
 * Executes business logic synchronously or asynchronously using a [Scheduler].
 */
abstract class UseCase<in P, R> {

    private val taskScheduler = DefaultScheduler

    /** Executes the use case asynchronously
     *
     * @return an observable [LiveData] with a [Result].
     *
     * @param parameters the input parameters to run the use case with
     */
    fun executeAsync(parameters: P) : LiveData<Result<R>> {
        val liveCallback: MutableLiveData<Result<R>> = MutableLiveData()
        try {
            taskScheduler.execute {
                execute(parameters).let { result ->
                    liveCallback.postValue(Result.Success(result))
                }
            }
        }
        catch (e: Exception) {
            Timber.d(e)
            liveCallback.postValue(Result.Error(e))
        }
        return liveCallback
    }

    /** Executes the use case synchronously  */
    fun executeNow(parameters: P): Result<R> {
        return try {
            Result.Success(execute(parameters))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Override this to set the code to be executed.
     */
    @Throws(RuntimeException::class)
    protected abstract fun execute(parameters: P): R
}

