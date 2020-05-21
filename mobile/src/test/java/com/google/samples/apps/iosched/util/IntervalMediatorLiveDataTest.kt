/*
 * Copyright 2020 Google LLC
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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.google.samples.apps.iosched.shared.util.IntervalMediatorLiveData
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class IntervalMediatorLiveDataTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var testDispatcher: TestCoroutineDispatcher
    private lateinit var sourceLiveData: MutableLiveData<Unit>
    private lateinit var subject: LiveData<Int>

    @Before
    fun setup() {
        testDispatcher = TestCoroutineDispatcher()
        sourceLiveData = MutableLiveData()

        var calls = 0
        subject = IntervalMediatorLiveData(
            source = sourceLiveData,
            dispatcher = testDispatcher,
            intervalMs = 1_000L
        ) {
            ++calls
        }
    }

    @After
    fun teardown() {
        Assert.assertFalse(subject.hasObservers())
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `when not observed then transformation is not called`() {
        testDispatcher.advanceTimeBy(10_000L)
        Assert.assertNull(subject.value)
    }

    @Test
    fun `when observed and source changes then transformation is called`() {
        // Source has no value yet.
        Assert.assertNull(subject.value)

        sourceLiveData.postValue(Unit)
        val observer = Observer<Int> {}
        subject.observeForever(observer)
        // Transformation called immediately because sourceLiveData has a value
        Assert.assertEquals(1, subject.value)

        // Post a value to source, but don't advance time.
        sourceLiveData.postValue(Unit)
        Assert.assertEquals(2, subject.value)

        // Cleanup
        subject.removeObserver(observer)
    }

    @Test
    fun `when observed then transformation called every interval`() {
        val observer = Observer<Int> {}
        subject.observeForever(observer)
        // Source has no value yet.
        Assert.assertNull(subject.value)

        // Advance time. Note: don't post value to source.
        testDispatcher.advanceTimeBy(1_000L)
        Assert.assertEquals(1, subject.value)

        // Half interval, no change.
        testDispatcher.advanceTimeBy(500L)
        Assert.assertEquals(1, subject.value)
        // Complete the interval.
        testDispatcher.advanceTimeBy(500L)
        Assert.assertEquals(2, subject.value)

        // Cleanup
        subject.removeObserver(observer)
    }

    @Test
    fun `when not observed then interval pauses`() {
        val observer = Observer<Int> {}
        subject.observeForever(observer)
        // Source has no value yet.
        Assert.assertNull(subject.value)

        sourceLiveData.postValue(Unit)
        Assert.assertEquals(1, subject.value)
        testDispatcher.advanceTimeBy(1_000L)
        Assert.assertEquals(2, subject.value)

        subject.removeObserver(observer)
        // Posting changes to source and advancing time has no effect.
        sourceLiveData.postValue(Unit)
        testDispatcher.advanceTimeBy(10_000L)
        Assert.assertEquals(2, subject.value)

        subject.observeForever(observer)
        // Transformation called immediately because source has new value.
        Assert.assertEquals(3, subject.value)

        testDispatcher.advanceTimeBy(1_000L)
        Assert.assertEquals(4, subject.value)

        // Cleanup
        subject.removeObserver(observer)
    }
}
