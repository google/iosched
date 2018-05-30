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

package com.google.samples.apps.iosched.tv.search

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.media.Rating
import android.net.Uri
import android.provider.BaseColumns
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.shared.domain.search.SearchUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.tv.TvApplication
import timber.log.Timber
import javax.inject.Inject

private const val AUTHORITY = "com.google.samples.apps.iosched.tv"
// UriMatcher constant for search suggestions
private const val SEARCH_SUGGEST = 1

/**
 * Provides global search for I/O sessions. The assistant will query this provider for results.
 *
 * This class enables
 * [on-device search.](https://developer.android.com/training/tv/discovery/searchable.html).
 */
class SessionContentProvider : ContentProvider() {

    @Inject lateinit var searchUseCase: SearchUseCase

    private lateinit var mUriMatcher: UriMatcher

    private val queryProjection = arrayOf(
        BaseColumns._ID,
        SearchManager.SUGGEST_COLUMN_TEXT_1, // Session name
        SearchManager.SUGGEST_COLUMN_TEXT_2, // Session Description
        SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE, // Session Icon
        SearchManager.SUGGEST_COLUMN_CONTENT_TYPE,
        SearchManager.SUGGEST_COLUMN_IS_LIVE,
        SearchManager.SUGGEST_COLUMN_VIDEO_WIDTH,
        SearchManager.SUGGEST_COLUMN_VIDEO_HEIGHT,
        SearchManager.SUGGEST_COLUMN_AUDIO_CHANNEL_CONFIG, // "2.0"?
        SearchManager.SUGGEST_COLUMN_PURCHASE_PRICE, // Free!
        SearchManager.SUGGEST_COLUMN_RENTAL_PRICE,
        SearchManager.SUGGEST_COLUMN_RATING_STYLE, // TODO: gather this from feedback?
        SearchManager.SUGGEST_COLUMN_RATING_SCORE, // TODO: gather this from feedback?
        SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR,
        SearchManager.SUGGEST_COLUMN_DURATION,
        SearchManager.SUGGEST_COLUMN_INTENT_ACTION, // "GLOBALSEARCH"
        SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
    )

    override fun onCreate(): Boolean {
        (context.applicationContext as TvApplication).searchableComponent
            .inject(sessionContentProvider = this)
        mUriMatcher = buildUriMatcher()
        return true
    }

    private fun buildUriMatcher(): UriMatcher {
        val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        uriMatcher.addURI(
            AUTHORITY, "/search/" + SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST
        )
        uriMatcher.addURI(
            AUTHORITY,
            "/search/" + SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
            SEARCH_SUGGEST
        )
        return uriMatcher
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {

        Timber.d("Raw query string supplied: $uri")

        if (mUriMatcher.match(uri) == SEARCH_SUGGEST) {
            Timber.d("Search suggestions requested.")
            return search(uri.lastPathSegment)
        } else {
            Timber.d("Unknown uri to query: $uri")
            throw IllegalArgumentException("Unknown Uri: $uri")
        }
    }

    /**
     * Executes the [SearchUseCase] and converts the results into a cursor for the Google Assistant
     * to render as the results.
     */
    private fun search(query: String): Cursor {

        val result: Result<List<Session>> = searchUseCase.executeNow(query)

        val sessions = when (result) {
            is Result.Error -> {
                // If there is an error, fail silently and return no results to the Google
                // Assistant.
                Timber.e(result.exception, "Could not retrieve search results for query: $query")
                emptyList()
            }
            is Result.Success -> result.data
            else -> emptyList()
        }

        val matrixCursor = MatrixCursor(queryProjection)

        if (sessions.isEmpty()) {
            // If there are no sessions, return empty results.
            return matrixCursor
        }

        sessions
            .map { session: Session -> convertMovieIntoRow(session) }
            .forEach({ row -> matrixCursor.addRow(row) })

        Timber.d("Returning ${matrixCursor.count} results for query `$query`")
        return matrixCursor
    }

    private fun convertMovieIntoRow(session: Session): Array<Any> {
        return arrayOf(
            session.id,
            session.title,
            session.abstract,
            session.photoUrl ?: "",
            "video/mp4", // TODO: get the content type of the videos.
            session.isLive(),
            0, // TODO: get video width
            0, // TODO: get video height
            "2.0", // TODO: get audio channel configuration
            "Free",
            "Free",
            Rating.RATING_NONE,
            0f,
            session.year,
            session.duration,
            "GLOBALSEARCH",
            session.id
        )
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri {
        throw UnsupportedOperationException("Insert is not implemented.")
    }

    override fun delete(uri: Uri, string: String?, strings: Array<String>?): Int {
        throw UnsupportedOperationException("Delete is not implemented.")
    }

    override fun update(
        uri: Uri,
        contentValues: ContentValues?,
        string: String?,
        strings: Array<String>?
    ): Int {
        throw UnsupportedOperationException("Update is not implemented.")
    }
}
