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
import com.google.samples.apps.iosched.shared.data.feed.FeedRepository
import com.google.samples.apps.iosched.shared.domain.feed.LoadFeedUseCase
import com.google.samples.apps.iosched.shared.model.FeedItem
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for [LoadFeedUseCase]
 */
class LoadFeedUseCaseTest {

    @Test
    fun feedItemsLoadedSuccessfully() {
        val useCase = LoadFeedUseCase(SuccessfulFeedRepository)

        val resultLivedata = useCase.observe()

        useCase.execute(Unit)

        val result = LiveDataTestUtil.getValue(resultLivedata)
        Assert.assertEquals(result, Result.Success(TestFeedDataSource.feedItems))
    }

    @Test
    fun feedItemsLoadedUnsuccessfully() {
        val useCase = LoadFeedUseCase(UnsuccessfulFeedRepository)

        val resultLivedata = useCase.observe()

        useCase.execute(Unit)

        val result = LiveDataTestUtil.getValue(resultLivedata)
        Assert.assertEquals(result, Result.Error(Exception("Error!")))
    }

}

val SuccessfulFeedRepository = object : FeedRepository {
    override fun clearSubscriptions() {}

    val dataSource = TestFeedDataSource
    override fun getObservableFeedItems(): LiveData<Result<List<FeedItem>>> {
        return dataSource.getObservableFeedItems()
    }
}

val UnsuccessfulFeedRepository = object : FeedRepository {
    override fun clearSubscriptions() {}

    private val feedResults: MutableLiveData<Result<List<FeedItem>>> = MutableLiveData()

    override fun getObservableFeedItems(): LiveData<Result<List<FeedItem>>> {
        feedResults.postValue(Result.Error(Exception("Error!")))
        return feedResults
    }

}