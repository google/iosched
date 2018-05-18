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

import androidx.lifecycle.LiveData
import com.google.samples.apps.iosched.shared.util.SetIntervalLiveData
import com.google.samples.apps.iosched.shared.util.SetIntervalLiveData.DefaultIntervalMapper
import com.google.samples.apps.iosched.shared.util.SetIntervalLiveData.IntervalMapper
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Rule for [IntervalMapper] in tests to execute the map function immediately without waiting for
 * the interval.
 */
class FakeIntervalMapperRule : TestWatcher() {

    override fun starting(description: Description?) {
        super.starting(description)
        DefaultIntervalMapper.setDelegate(FakeIntervalMapper())
    }

    override fun finished(description: Description?) {
        super.finished(description)
        DefaultIntervalMapper.setDelegate(null)
    }
}

class FakeIntervalMapper : IntervalMapper {
    override fun <P, R> mapAtInterval(
        source: LiveData<P>,
        interval: Long,
        map: (P?) -> R?
    ): SetIntervalLiveData<P, R> {
        return FakeSetIntervalLiveData(source, interval, map)
    }
}

/**
 * Fake implementation of [SetIntervalLiveData] where the map function is executed
 * immediately.
 */
class FakeSetIntervalLiveData<in P, R>(
    source: LiveData<P>,
    intervalMs: Long,
    map: (P?) -> R?
) : SetIntervalLiveData<P, R>(source, intervalMs, map) {
    init {
        value = map(source.value)
    }
}
