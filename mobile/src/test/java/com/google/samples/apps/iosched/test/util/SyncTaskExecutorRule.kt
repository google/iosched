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

package com.google.samples.apps.iosched.test.util

import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.domain.internal.SyncScheduler
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Rule to be used in tests that sets a synchronous task scheduler used to avoid race conditions.
 */
class SyncTaskExecutorRule : TestWatcher() {
    override fun starting(description: Description?) {
        super.starting(description)
        DefaultScheduler.setDelegate(SyncScheduler)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        SyncScheduler.clearScheduledPostdelayedTasks()
        DefaultScheduler.setDelegate(null)
    }

    /**
     * Force the (previously deferred) execution of all [Scheduler.postDelayedToMainThread] tasks.
     *
     * In tests, postDelayed is not eagerly executed, allowing test code to test self-scheduling
     * tasks.
     *
     * This will *not* run any tasks that are scheduled as a result of running the current delayed
     * tasks. If you need to test that more tasks were scheduled, call this function again.
     */
    fun runAllScheduledPostDelayedTasks() {
        SyncScheduler.runAllScheduledPostDelayedTasks()
    }
}
