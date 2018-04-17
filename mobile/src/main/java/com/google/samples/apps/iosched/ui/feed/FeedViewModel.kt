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

package com.google.samples.apps.iosched.ui.feed

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.domain.feed.LoadFeedUseCase
import com.google.samples.apps.iosched.shared.model.FeedItem
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.SnackbarMessage
import javax.inject.Inject


/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class FeedViewModel @Inject constructor(
        private val loadFeedUseCase: LoadFeedUseCase) : ViewModel() {

    val errorMessage: LiveData<Event<String>>

    val feed: LiveData<List<FeedItem>>

    val isLoading: LiveData<Boolean>

    val snackBarMessage: LiveData<Event<SnackbarMessage>>

    private val loadFeedResult: MediatorLiveData<Result<List<FeedItem>>> = loadFeedUseCase.observe()

    init {

        feed = loadFeedResult.map {
            (it as? Result.Success)?.data ?: emptyList()
        }

        isLoading = loadFeedResult.map { it == Result.Loading }

        errorMessage = loadFeedResult.map {
            Event(content = (it as? Result.Error)?.exception?.message ?: "")
        }

        // Show an error message if the feed could not be loaded.
        snackBarMessage = MediatorLiveData()
        snackBarMessage.addSource(loadFeedResult) {
            if (it is Result.Error) {
                snackBarMessage.postValue(Event(SnackbarMessage(
                        messageId = R.string.feed_loading_error,
                        longDuration = true
                )))
            }
        }

    }

    override fun onCleared() {
        // Clear subscriptions that might be leaked or that will not be used in the future.
        loadFeedUseCase.onCleared()
    }

    fun loadFeed() {
        loadFeedUseCase.execute(Unit)
    }

}
