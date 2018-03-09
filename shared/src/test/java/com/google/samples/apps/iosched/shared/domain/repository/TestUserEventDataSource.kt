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

package com.google.samples.apps.iosched.shared.domain.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.TestData
import com.google.samples.apps.iosched.shared.result.Result

object TestUserEventDataSource : UserEventDataSource {

    override fun getObservableUserEvents(userId: String): LiveData<UserEventsResult> {
        val result = MutableLiveData<UserEventsResult>()
        result.postValue(UserEventsResult(true, TestData.userEvents))
        return result
    }

    override fun updateStarred(userId: String, session: Session, isStarred: Boolean):
            LiveData<Result<Boolean>> {
        val result = MutableLiveData<Result<Boolean>>()
        result.postValue(Result.Success(true))
        return result
    }

    override fun getUserEvents(userId: String) = TestData.userEvents
}