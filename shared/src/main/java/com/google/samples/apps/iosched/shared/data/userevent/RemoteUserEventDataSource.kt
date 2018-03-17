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
import android.arch.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryListenOptions
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.toEpochMilli
import javax.inject.Inject

class RemoteUserEventDataSource @Inject constructor(
        val firestore: FirebaseFirestore
) : UserEventDataSource {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val EVENTS_COLLECTION = "events"
        private const val ID = "id"
        private const val START_TIME = "startTime"
        private const val END_TIME = "endTime"
        private const val IS_STARRED = "isStarred"
    }

    override fun getUserEvents(userId: String): List<UserEvent> {
        if (userId.isEmpty()) {
            return emptyList()
        }
        val task = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION).get()
        val snapshot = Tasks.await(task)
        return snapshot.documents.map {
            UserEvent(id = it.id,
                    startTime = it[START_TIME] as Long,
                    endTime = it[END_TIME] as Long,
                    isStarred = it[IS_STARRED] as Boolean)
        }
    }

    override fun getObservableUserEvents(userId: String): LiveData<UserEventsResult> {
        val result = MutableLiveData<UserEventsResult>()
        // Need to include this option to let the Metadata#hasPendingWrite() is called
        // When there is locally modified data, which isn't synced to the server
        val options = QueryListenOptions().includeDocumentMetadataChanges()
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION).addSnapshotListener(options, { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener

                    if (snapshot.metadata.hasPendingWrites()) {
                        // This means locally modified data isn't synced to the server
                        val changedIds =
                                snapshot.documentChanges.map { it.document.data[ID] }.toSet()
                        result.postValue(UserEventsResult(false, snapshot.documents.map {
                            UserEvent(id = it.id,
                                    startTime = it[START_TIME] as? Long ?: 0,
                                    endTime = it[END_TIME] as? Long ?: 0,
                                    isStarred = it[IS_STARRED] as? Boolean ?: false).apply {
                                hasPendingWrite = it[ID] in changedIds
                            }
                        }))
                    } else {
                        result.postValue(
                                UserEventsResult(true, snapshot.documents.map {
                                    UserEvent(id = it.id,
                                            startTime = it[START_TIME] as? Long ?: 0,
                                            endTime = it[END_TIME] as? Long ?: 0,
                                            isStarred = it[IS_STARRED] as? Boolean ?: false).apply {
                                        hasPendingWrite = false
                                    }
                                }))
                    }
                })
        return result
    }

    override fun updateStarred(userId: String, session: Session, isStarred: Boolean):
            LiveData<Result<Boolean>> {
        val result = MutableLiveData<Result<Boolean>>()
        val data = mapOf(ID to session.id,
                START_TIME to session.startTime.toEpochMilli(),
                END_TIME to session.endTime.toEpochMilli(),
                IS_STARRED to isStarred)
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(session.id).set(data).addOnCompleteListener({
                    if (it.isSuccessful) {
                        result.postValue(Result.Success(true))
                    } else {
                        result.postValue(Result.Error(
                                it.exception ?: RuntimeException("Error updating star.")))
                    }
                })
        return result
    }
}


