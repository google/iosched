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

package com.google.samples.apps.iosched.tests

import com.google.samples.apps.iosched.shared.time.DefaultTimeProvider
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime

/**
 * Rule to be used in tests that sets the clocked used by DefaultTimeProvider.
 */
class FixedTimeRule(
    private val fixedTime: FixedTimeProvider = FixedTimeProvider(1_000_000)
) : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        DefaultTimeProvider.setDelegate(fixedTime)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        DefaultTimeProvider.setDelegate(null)
    }
}

/**
 * Fix the TimeProvider to a fixed time
 */
class FixedTimeProvider(private var time: ZonedDateTime) : TimeProvider {
    constructor(timeInMilis: Long) : this(Instant.ofEpochMilli(timeInMilis))
    constructor(instant: Instant) : this(
        ZonedDateTime.ofInstant(instant, TimeUtils.CONFERENCE_TIMEZONE)
    )

    override fun now(): Instant {
        return time.toInstant()
    }
    override fun nowZoned(): ZonedDateTime {
        return time
    }
}
