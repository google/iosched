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

package com.google.samples.apps.iosched.test.util.time

import com.google.samples.apps.iosched.util.time.DefaultTime
import com.google.samples.apps.iosched.util.time.MockableTime
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.threeten.bp.Instant

/**
 * Fix the MockableTime to a fixed time
 */
class FixedTime(var instant: Instant) : MockableTime {
    constructor(timeInMilis: Long) : this(Instant.ofEpochMilli(timeInMilis))

    override fun now(): Instant {
        return instant
    }
}

/**
 * Rule to be used in tests that sets the clocked used by DefaultTime.
 */
class FixedTimeExecutorRule(val fixedTime: FixedTime = FixedTime(1_000_000)) : TestWatcher() {

    var time: Instant
        get() = fixedTime.instant
        set(value) {
            fixedTime.instant = value
        }

    override fun starting(description: Description?) {
        super.starting(description)
        DefaultTime.setDelegate(fixedTime)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        DefaultTime.setDelegate(null)
    }
}
