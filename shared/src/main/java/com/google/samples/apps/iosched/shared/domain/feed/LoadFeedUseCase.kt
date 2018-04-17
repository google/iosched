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

package com.google.samples.apps.iosched.shared.domain.feed

import com.google.samples.apps.iosched.shared.data.feed.FeedRepository
import com.google.samples.apps.iosched.shared.domain.MediatorUseCase
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.model.FeedItem
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject

/**
 * Loads all feed items into a list.
 */
open class LoadFeedUseCase @Inject constructor(
        private val repository: FeedRepository
) : MediatorUseCase<Unit, List<FeedItem>>() {

    override fun execute(parameters: Unit) {
        result.postValue(Result.Loading)
        val feedObservable = repository.getObservableFeedItems()

        result.removeSource(feedObservable)
        result.value = null
        result.addSource(feedObservable) {
            DefaultScheduler.execute {
                when (it) {
                    is Result.Success -> {
                        val feedItems = it.data
                        result.postValue(Result.Success(feedItems))
                    }
                    is Result.Error -> {
                        result.postValue(it)
                    }
                }

            }
        }
    }

    fun onCleared() {
        // This use case is no longer going to be used so remove subscriptions
        repository.clearSubscriptions()
    }
}