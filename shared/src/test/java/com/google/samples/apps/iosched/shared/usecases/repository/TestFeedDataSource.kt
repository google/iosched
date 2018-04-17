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

package com.google.samples.apps.iosched.shared.usecases.repository

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.shared.data.feed.FeedDataSource
import com.google.samples.apps.iosched.shared.model.FeedItem
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.TimeUtils

/**
 * Generates dummy session data to be used in tests.
 */
object TestFeedDataSource : FeedDataSource {
    private val feedResults: MutableLiveData<Result<List<FeedItem>>> = MutableLiveData()

    override fun getObservableFeedItems(): LiveData<Result<List<FeedItem>>> {
        feedResults.postValue(Result.Success(feedItems))
        return feedResults
    }

    override fun clearSubscriptions() {}

    private val feedItem1 = FeedItem(id = "0", title = "Item 1", message = "",
            timestamp = TimeUtils.ConferenceDay.DAY_1.start, imageUrl = "", color = 0,
            category = "", priority = false, emergency = true)

    private val feedItem2 = FeedItem(id = "1", title = "Item 2", message = "",
            timestamp = TimeUtils.ConferenceDay.DAY_1.end, imageUrl = "", color = 0,
            category = "", priority = true, emergency = false)

    private val feedItem3 = FeedItem(id = "2", title = "Item 3", message = "",
            timestamp = TimeUtils.ConferenceDay.DAY_2.start, imageUrl = "", color = 0,
            category = "", priority = false, emergency = false)

    private val feedItem4 = FeedItem(id = "3", title = "Item 4", message = "",
            timestamp = TimeUtils.ConferenceDay.DAY_2.end, imageUrl = "", color = 0,
            category = "", priority = false, emergency = false)

    val feedItems = listOf(feedItem1, feedItem2, feedItem3, feedItem4)

}