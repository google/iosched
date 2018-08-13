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

package com.google.samples.apps.iosched.util

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.shared.util.SetIntervalLiveData.DefaultIntervalMapper
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SetIntervalLiveDataTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun testBasicMapOperation() {
        val source = MutableLiveData<Int>()
        source.value = 5

        val subject = DefaultIntervalMapper.mapAtInterval(source) { sourceValue ->
            sourceValue?.run {
                sourceValue + 5
            }
        }

        assertEquals(10, LiveDataTestUtil.getValue(subject))
    }

    @Test
    fun whenNoIntervalsTriggered_thenMapFn_isOnlyCalledOnce() {
        val source = MutableLiveData<Int>()
        source.value = 5

        var calls = 0

        val subject = DefaultIntervalMapper.mapAtInterval(source) {
            ++calls
        }

        assertEquals(1, LiveDataTestUtil.getValue(subject))
    }

    @Test
    fun whenNoIntervalsTriggered_thenMapFn_isCalledWhenNewValue() {
        val source = MutableLiveData<Int>()
        source.value = 5

        var calls = 0

        val subject = DefaultIntervalMapper.mapAtInterval(source) {
            ++calls
        }

        LiveDataTestUtil.getValue(subject) // register observer so it processes setValue

        source.value = 10

        assertEquals(2, LiveDataTestUtil.getValue(subject))
    }

    @Test
    fun whenSourceHasEmittedSeveral_thenMapFnIsCalledWithLastData() {
        val source = MutableLiveData<Int>()
        source.value = 5
        source.value = 6
        source.value = 7
        source.value = 8

        var calls = 0

        val subject = DefaultIntervalMapper.mapAtInterval(source) { sourceValue ->
            sourceValue?.run {
                calls++
                sourceValue + 5
            }
        }

        assertEquals(13, LiveDataTestUtil.getValue(subject))
        assertEquals(1, calls)
    }

    @Test
    fun whenIntervalTriggered_mapRunsOnLastValue() {
        val source = MutableLiveData<Int>()
        source.value = 5

        var timeModifier = 5

        val subject = DefaultIntervalMapper.mapAtInterval(source) { sourceValue ->
            sourceValue?.run {
                sourceValue + timeModifier
            }
        }

        val observer = Observer<Int>({})

        // make the subject active so it runs intervals
        subject.observeForever(observer)

        timeModifier = 10
        syncTaskExecutorRule.runAllScheduledPostDelayedTasks()

        assertEquals(15, LiveDataTestUtil.getValue(subject))

        subject.removeObserver(observer)
    }

    @Test
    fun whenNotActive_mapDoesNotRunOnInterval() {
        val source = MutableLiveData<Int>()
        source.value = 5

        var calls = 0

        val subject = DefaultIntervalMapper.mapAtInterval(source) {
            ++calls
        }
        assertEquals(1, LiveDataTestUtil.getValue(subject))

        syncTaskExecutorRule.runAllScheduledPostDelayedTasks()

        assertEquals(1, LiveDataTestUtil.getValue(subject))
    }
}
