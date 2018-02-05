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
import android.support.v17.leanback.app.BrowseFragment
import android.support.v17.leanback.app.BrowseSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ListRowPresenter
import android.support.v17.leanback.widget.PageRow
import android.support.v17.leanback.widget.Row
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import com.google.samples.apps.iosched.tv.R
import com.google.samples.apps.iosched.tv.ui.schedule.ScheduleFragment

// TODO: Replace these constants with a viewModel
private const val HEADER_ID_1: Long = 1
private const val HEADER_NAME_1 = "Day 1"
private const val HEADER_ID_2: Long = 2
private const val HEADER_NAME_2 = "Day 2"
private const val HEADER_ID_3: Long = 3
private const val HEADER_NAME_3 = "Day 3"
private const val HEADER_ID_4: Long = 4
private const val HEADER_NAME_4 = "Map"

class MainFragment : BrowseSupportFragment() {

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val context = activity!!.applicationContext

        title = getString(R.string.browse_title)
        badgeDrawable = ContextCompat.getDrawable(context, R.drawable.ic_banner)
        headersState = BrowseFragment.HEADERS_ENABLED
        isHeadersTransitionOnBackEnabled = true

        adapter = rowsAdapter

        loadData()

        mainFragmentRegistry.registerFragment(PageRow::class.java, PageRowFragmentFactory())
    }

    private fun loadData() {
        // TODO: this is dummy data, replace with loading a viewModel.
        val headerItem1 = HeaderItem(HEADER_ID_1, HEADER_NAME_1)
        val pageRow1 = PageRow(headerItem1)
        rowsAdapter.add(pageRow1)

        val headerItem2 = HeaderItem(HEADER_ID_2, HEADER_NAME_2)
        val pageRow2 = PageRow(headerItem2)
        rowsAdapter.add(pageRow2)

        val headerItem3 = HeaderItem(HEADER_ID_3, HEADER_NAME_3)
        val pageRow3 = PageRow(headerItem3)
        rowsAdapter.add(pageRow3)

        val headerItem4 = HeaderItem(HEADER_ID_4, HEADER_NAME_4)
        val pageRow4 = PageRow(headerItem4)
        rowsAdapter.add(pageRow4)
    }

    private inner class PageRowFragmentFactory : BrowseSupportFragment.FragmentFactory<Fragment>() {

        override fun createFragment(rowObj: Any): Fragment {
            val row = rowObj as Row
            return when (row.headerItem.id) {
                HEADER_ID_1, HEADER_ID_2, HEADER_ID_3, HEADER_ID_4 -> ScheduleFragment()
                else -> throw IllegalArgumentException("Invalid row $rowObj")
            }
        }
    }
}
