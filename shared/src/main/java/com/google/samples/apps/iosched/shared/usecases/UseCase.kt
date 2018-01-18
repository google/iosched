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

import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.usecases.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.usecases.internal.Scheduler
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Executes business logic synchronously or asynchronously using a [Scheduler].
 */
abstract class UseCase<in P, out R> {

    private val taskScheduler = DefaultScheduler

    /* The callback is stored with a weak reference to prevent leaks. */
    private lateinit var callback: WeakReference<(Result<R>) -> Any>

    /** Executes the use case asynchronously  */
    fun executeAsync(parameters: P, callback: (Result<R>) -> Any) {
        this.callback = WeakReference(callback)

        taskScheduler.execute {
            // Run in background
            try {
                execute(parameters)?.let {
                    notifyResult(it)
                } ?: notifyError(NullPointerException("Result was null"))
            } catch (e: Exception) {
                Timber.d(e)
                notifyError(e)
            }
        }

    }

    /** Executes the use case synchronously  */
    fun executeNow(parameters: P): Result<R> {
        return try {
            Result.Success(execute(parameters))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun notifyResult(result: R) {
        callback.get()?.let {
            taskScheduler.postToMainThread {
                // Double check because of WeakRef
                val safeCallback: (Result<R>) -> Any = callback.get() ?: return@postToMainThread
                safeCallback(Result.Success(result))
            }
        }
    }

    private fun notifyError(e: Exception) {
        callback.get()?.let {
            taskScheduler.postToMainThread {
                // Double check because of WeakRef
                val safeCallback: (Result<R>) -> Any = callback.get() ?: return@postToMainThread
                safeCallback(Result.Error(e))
            }
        }
    }

    /**
     * Override this to set the code to be executed.
     */
    @Throws(RuntimeException::class)
    protected abstract fun execute(parameters: P): R
}

