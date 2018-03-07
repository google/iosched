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

package com.google.samples.apps.iosched.shared.util

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test

class WeakLiveDataHolderTest {
    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun allObserversAreNotified_onNotifyAll() {
        val subject = WeakLiveDataHolder<String>()

        val observer1 = MutableLiveData<String>()
        val observer2 = MutableLiveData<String>()

        subject.addLiveDataObserver(observer1)
        subject.addLiveDataObserver(observer2)

        val expected = "an update"
        subject.notifyAll(expected)

        assertThat(observer1, `is`(not(observer2)))
        assertThat(observer1.value, `is`(expected))
        assertThat(observer2.value, `is`(expected))
    }

    @Test
    fun whenObserverAdded_previousValuesArePosted() {
        val subject = WeakLiveDataHolder<String>()
        val expected = "an update"
        subject.notifyAll(expected)

        val observer = MutableLiveData<String>()
        subject.addLiveDataObserver(observer)

        assertThat(observer.value, `is`(expected))
    }

    @Test
    fun notifyWhenChanged_onlyNotifiesForChanges() {
        val subject = WeakLiveDataHolder<String>()

        val observer = MutableLiveData<String>()
        subject.addLiveDataObserver(observer)

        val observed = mutableListOf<String>()
        observer.observeForever { observed.add(it!!) }

        subject.notifyIfChanged("a")
        subject.notifyIfChanged("a")
        subject.notifyIfChanged("b")
        subject.notifyIfChanged("c")

        assertThat(observed, `is`(equalTo(listOf("a", "b", "c"))))
    }
}
