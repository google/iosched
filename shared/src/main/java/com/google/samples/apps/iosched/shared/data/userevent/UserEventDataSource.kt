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

package com.google.samples.apps.iosched.shared.data.userevent

import android.arch.lifecycle.LiveData
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result

interface UserEventDataSource {

    fun getUserEvents(userId: String): List<UserEvent>

    fun getObservableUserEvents(userId: String): LiveData<UserEventsResult>

    /**
     * Toggle the isStarred status for an event.
     *
     * @param userId the userId ([FirebaseUser#uid]) of the current logged in user
     * @param session the event being toggled
     * @param isStarred the updated state whether the event is starred
     * @return the LiveData that represents the status of the star operation.
     */
    fun updateStarred(userId: String, session: Session, isStarred: Boolean):
            LiveData<Result<Boolean>>
}

data class UserEventsResult(
        /** If this is true, all [UserEvent] in the userEvents field are synced to the backend */
        val allDataSynced: Boolean,
        val userEvents: List<UserEvent>)
