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

package com.google.samples.apps.iosched.shared.time

import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime

interface TimeProvider {
    fun now(): Instant
    fun nowZoned(): ZonedDateTime
}

object DefaultTimeProvider : TimeProvider {
    private var delegate: TimeProvider = WallclockTimeProvider

    fun setDelegate(newDelegate: TimeProvider?) {
        delegate = newDelegate ?: WallclockTimeProvider
    }

    override fun now(): Instant {
        return delegate.now()
    }
    override fun nowZoned(): ZonedDateTime {
        return delegate.nowZoned()
    }
}

internal object WallclockTimeProvider : TimeProvider {
    override fun nowZoned(): ZonedDateTime = ZonedDateTime.now()

    override fun now(): Instant = Instant.now()
}
