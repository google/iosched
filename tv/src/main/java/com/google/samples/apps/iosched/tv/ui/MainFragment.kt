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

package com.google.samples.apps.iosched.tv.ui

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.PageRow
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.shared.util.getThemeColor
import com.google.samples.apps.iosched.tv.R

class MainFragment : BrowseSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val context = requireActivity().applicationContext

        title = getString(R.string.browse_title)
        badgeDrawable = ContextCompat.getDrawable(context, R.drawable.ic_banner)
        headersState = BrowseSupportFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true
        brandColor = context.getThemeColor(R.attr.colorPrimary, R.color.indigo)

        adapter = rowsAdapter

        loadData()

        mainFragmentRegistry.registerFragment(PageRow::class.java, MainPageRowFragmentFactory())
    }

    private fun loadData() {

        val days = ConferenceDays
        days.forEachIndexed { index, day ->

            val displayDate = day.formatMonthDay()

            val headerItem = HeaderItem(index.toLong(), displayDate)
            val pageRow = PageRow(headerItem)
            rowsAdapter.add(pageRow)
        }
    }
}
