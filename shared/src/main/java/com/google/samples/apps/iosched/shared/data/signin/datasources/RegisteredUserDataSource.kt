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

package com.google.samples.apps.iosched.shared.data.signin.datasources

import androidx.lifecycle.LiveData
import com.google.samples.apps.iosched.shared.result.Result

/**
 * A data source that listens to changes in the user data related to event
 * registration.
 */
interface RegisteredUserDataSource {
    /**
     * Listens to changes in the user document in Firestore. A Change in the "registered" field
     * will emit a new user.
     */
    fun listenToUserChanges(userId: String)

    /**
     * Returns the holder of the result of listening to the data source.
     */
    fun observeResult(): LiveData<Result<Boolean?>?>

    /**
     * Clear listeners and set the result of the observable to false when the user is not signed in.
     */
    fun setAnonymousValue()
}
