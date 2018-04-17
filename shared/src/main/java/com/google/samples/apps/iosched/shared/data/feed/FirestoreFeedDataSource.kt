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
import android.arch.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.model.FeedItem
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.ColorUtils
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

interface FeedDataSource {
    fun getObservableFeedItems(): LiveData<Result<List<FeedItem>>>
    fun clearSubscriptions()
}

/**
 * Feed data source backed by items in a FireStore collection.
 */
class FirestoreFeedDataSource @Inject constructor(
        val firestore: FirebaseFirestore
) : FeedDataSource {

    override fun getObservableFeedItems(): MutableLiveData<Result<List<FeedItem>>> {
        registerListenerForEvents(resultFeed)
        return resultFeed
    }

    private var feedChangedListenerSubscription: ListenerRegistration? = null

    private fun registerListenerForEvents(result: MutableLiveData<Result<List<FeedItem>>>) {
        val eventsListener: (QuerySnapshot?, FirebaseFirestoreException?) -> Unit =
                listener@{ snapshot, _ ->
                    snapshot ?: return@listener

                    DefaultScheduler.execute {
                        try {
                            Timber.d("Feed items change detected: ${snapshot.documentChanges.size}")

                            // Parse the document into feed items and sort them.
                            // First by priority, then timestamp.

                            val feedItemsResult = snapshot.documents.map { parseFeedItem(it) }
                                    .sortedWith(compareByDescending<FeedItem> { it.priority }
                                            .thenBy { it.timestamp })
                            result.postValue(Result.Success(feedItemsResult))
                        } catch (e: Exception) {
                            result.postValue(Result.Error(e))
                        }
                    }
                }

        // Only load items marked as active.
        val collection = firestore.collection(FEED_COLLECTION).whereEqualTo(ACTIVE, true)
        feedChangedListenerSubscription?.remove()
        feedChangedListenerSubscription = collection.addSnapshotListener(eventsListener)
    }

    override fun clearSubscriptions() {
        Timber.d("Firestore Feed data source: Clearing subscriptions")
        resultFeed.value = null
        feedChangedListenerSubscription?.remove()
    }

    fun parseFeedItem(snapshot: DocumentSnapshot): FeedItem {

        return FeedItem(id = snapshot.id,
                title = snapshot[FirestoreFeedDataSource.TITLE] as? String
                        ?: "",
                category = snapshot[FirestoreFeedDataSource.CATEGORY] as? String
                        ?: "",
                imageUrl = snapshot[FirestoreFeedDataSource.IMAGE_URL] as? String
                        ?: "",
                message = snapshot[FirestoreFeedDataSource.MESSAGE] as? String
                        ?: "",
                timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(
                        (snapshot[FirestoreFeedDataSource.TIMESTAMP] as? Date
                                ?: Date())
                                .time), ZoneOffset.UTC),
                color = ColorUtils.parseHexColor(snapshot[FirestoreFeedDataSource.COLOR] as? String
                        ?: ""),
                priority = snapshot[FirestoreFeedDataSource.PRIORITY] as? Boolean
                        ?: false,
                emergency = snapshot[FirestoreFeedDataSource.EMERGENCY] as? Boolean
                        ?: false
        )
    }

    companion object {
        /**
         * Firestore constants.
         */
        private const val FEED_COLLECTION = "feed"
        private const val ID = "id"
        private const val ACTIVE = "active"
        private const val CATEGORY = "category"
        private const val COLOR = "color"
        private const val TIMESTAMP = "timeStamp"
        private const val IMAGE_URL = "imageUrl"
        private const val MESSAGE = "message"
        private const val PRIORITY = "priority"
        private const val TITLE = "title"
        private const val EMERGENCY = "emergency"
    }

    // Observable feed items
    private val resultFeed = MutableLiveData<Result<List<FeedItem>>>()

}
