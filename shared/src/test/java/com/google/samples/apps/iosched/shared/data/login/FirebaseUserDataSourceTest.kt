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

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.samples.apps.iosched.shared.result.Result
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Rule
import org.junit.Test

class FirebaseUserDataSourceTest {

    @Rule
    @JvmField
    val instantTaskExecutor = InstantTaskExecutorRule()

    /**
     * This test is fairly long because the APIs are async.
     *
     * Making fakes is not possible because the classes have some abstract functions obfuscated
     * (e.g. FirebaseUser).
     */
    @Test
    fun watcherCallsCallback_withToken() {
        var actual: String? = null
        val expected = "a user token"

        val (firebase, idTask) = makeMockFirebaseAuth(expected)

        val subject = FirebaseUserDataSourceImpl(firebase)

        // call watch
        subject.watch {
            // and stash the value
            actual = it
        }

        performWatcherCallbacks(firebase, idTask)

        assertThat(actual, `is`(expected))
    }

    @Test
    fun watcherCallsObservables_withFirebaseUser() {
        val (firebase, idTask) = makeMockFirebaseAuth("a token")

        val expected = firebase.currentUser

        val subject = FirebaseUserDataSourceImpl(firebase)

        // call watch
        subject.watch {}

        val observer1 = MutableLiveData<Result<FirebaseUser?>>()
        val observer2 = MutableLiveData<Result<FirebaseUser?>>()

        subject.addObservableFirebaseUser(observer1)
        subject.addObservableFirebaseUser(observer2)

        var emit1: FirebaseUser? = null
        var emit2: FirebaseUser? = null

        observer1.observeForever {
            if (it is Result.Success) emit1 = it.data
        }
        observer2.observeForever {
            if (it is Result.Success) emit2 = it.data
        }
        performWatcherCallbacks(firebase, idTask)

        assertThat(observer1, `is`(not(observer2)))
        assertThat(emit1, `is`(expected))
        assertThat(emit2, `is`(expected))
    }

    @Test
    fun watchersAreCalled_forNullUsers() {
        val firebase = mock<FirebaseAuth> {
            on { currentUser }.doReturn(null as FirebaseUser?)
        }

        val subject = FirebaseUserDataSourceImpl(firebase)

        subject.watch {}

        val observer = MutableLiveData<Result<FirebaseUser?>>()
        subject.addObservableFirebaseUser(observer)

        var wasEmitted = 0

        observer.observeForever {
            if (it is Result.Success) wasEmitted++
        }

        assertThat(wasEmitted, `is`(equalTo(1)))

        // trigger onAuthStateChanged
        argumentCaptor<AuthStateListener>().apply {
            verify(firebase).addAuthStateListener(capture())
            lastValue.onAuthStateChanged(firebase)
        }

        assertThat(wasEmitted, `is`(equalTo(2)))
    }

    private fun performWatcherCallbacks(
        firebase: FirebaseAuth,
        idTask: Task<GetTokenResult>
    ) {
        // trigger onAuthStateChanged
        argumentCaptor<AuthStateListener>().apply {
            verify(firebase).addAuthStateListener(capture())
            lastValue.onAuthStateChanged(firebase)
        }

        argumentCaptor<OnCompleteListener<GetTokenResult>>().apply {
            verify(idTask).addOnCompleteListener(capture())
            lastValue.onComplete(idTask)
        }
    }

    private fun makeMockFirebaseAuth(expected: String): Pair<FirebaseAuth, Task<GetTokenResult>> {
        val tokenResult = GetTokenResult(expected)

        val mockTask = mock<Task<GetTokenResult>> {
            on { isSuccessful }.doReturn(true)
            on { result }.doReturn(tokenResult)
        }

        val mockUser = mock<FirebaseUser> {
            on { isAnonymous }.doReturn(false)
            on { getIdToken(false) }.doReturn(mockTask)
        }

        val firebase = mock<FirebaseAuth> {
            on { currentUser }.doReturn(mockUser)
        }
        return Pair(firebase, mockTask)
    }
}

