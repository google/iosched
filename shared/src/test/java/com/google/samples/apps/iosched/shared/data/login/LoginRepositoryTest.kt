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

package com.google.samples.apps.iosched.shared.data.login

import com.google.firebase.auth.FirebaseAuth
import com.google.samples.apps.iosched.test.util.fakes.FakeFirebaseUserDataSource
import com.google.samples.apps.iosched.test.util.fakes.FakeLoginDataSource
import com.nhaarman.mockito_kotlin.mock
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class LoginRepositoryTest {
    lateinit var fakeWatcher: FakeFirebaseUserDataSource
    lateinit var fakeLoginDataSource: FakeLoginDataSource
    lateinit var mockFirebase: FirebaseAuth

    lateinit var subject: LoginRepository

    @Before
    fun setUp() {
        fakeWatcher = FakeFirebaseUserDataSource()
        fakeLoginDataSource = FakeLoginDataSource()
        mockFirebase = mock()

        subject = LoginRepository(fakeLoginDataSource, fakeWatcher)
    }

    @Test
    fun watch_updatesUserForToken() {
        val expected = "expected result"
        fakeWatcher.onTokenChanged!!(expected)
        assertThat(fakeLoginDataSource.token, `is`(equalTo(expected)))
    }
}
