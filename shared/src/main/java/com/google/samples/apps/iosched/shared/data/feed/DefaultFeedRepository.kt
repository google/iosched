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

package com.google.samples.apps.iosched.shared.data.feed

import android.arch.lifecycle.LiveData
import com.google.samples.apps.iosched.shared.model.FeedItem
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single point of access to feed data for the presentation layer.
 */
interface FeedRepository {
    fun getObservableFeedItems(): LiveData<Result<List<FeedItem>>>
    fun clearSubscriptions()
}

@Singleton
open class DefaultFeedRepository @Inject constructor(
        private val dataSource: FeedDataSource
) : FeedRepository {

    override fun getObservableFeedItems(): LiveData<Result<List<FeedItem>>> =
            dataSource.getObservableFeedItems()

    override fun clearSubscriptions() {
        dataSource.clearSubscriptions()
    }

}
