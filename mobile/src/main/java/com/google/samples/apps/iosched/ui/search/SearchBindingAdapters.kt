/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.ui.search

import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.samples.apps.iosched.R

@BindingAdapter("searchResultIcon")
fun searchResultIcon(imageView: ImageView, type: String) {
    val iconId = when (type) {
        "session" -> R.drawable.ic_event
        "codelab" -> R.drawable.ic_agenda_codelab
        else -> R.drawable.ic_event
    }
    imageView.setImageDrawable(AppCompatResources.getDrawable(imageView.context, iconId))
}

@BindingAdapter(value = ["searchResultItems", "searchViewModel"], requireAll = true)
fun searchResultItems(
    recyclerView: RecyclerView,
    list: List<SearchResult>?,
    searchViewModel: SearchViewModel
) {
    if (recyclerView.adapter == null) {
        recyclerView.adapter = SearchAdapter(searchViewModel)
    }

    // TODO add case for no search term
    (recyclerView.adapter as SearchAdapter).submitList(list ?: emptyList())
}