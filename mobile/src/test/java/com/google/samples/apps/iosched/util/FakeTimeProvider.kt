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

package com.google.samples.apps.iosched.util

import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.test.data.TestData.TestConferenceDays
import org.threeten.bp.Instant
import org.threeten.bp.ZonedDateTime

object DayBeforeConferenceClock : TimeProvider {
    override fun now(): Instant {
        TODO("not implemented")
    }

    override fun nowZoned(): ZonedDateTime {
        return TimeUtils.ConferenceDays.first().start.plusHours(4).minusDays(1)
    }
}

object DayAfterConferenceClock : TimeProvider {
    override fun now(): Instant {
        TODO("not implemented")
    }

    override fun nowZoned(): ZonedDateTime {
        return TimeUtils.ConferenceDays.last().end.minusHours(4).plusDays(1)
    }
}

object FirstDayConferenceClock : TimeProvider {
    override fun now(): Instant {
        TODO("not implemented")
    }

    override fun nowZoned(): ZonedDateTime {
        return TestConferenceDays[0].start.plusHours(4)
    }
}
