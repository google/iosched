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

package com.google.samples.apps.iosched.tv.ui.sessiondetail

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v17.leanback.app.DetailsSupportFragment
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.ClassPresenterSelector
import android.support.v17.leanback.widget.DetailsOverviewRow
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.support.v17.leanback.widget.OnActionClickedListener
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.google.samples.apps.iosched.shared.data.BootstrapConferenceDataSource
import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.NetworkConferenceDataSource
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.util.viewModelProvider

import com.google.samples.apps.iosched.tv.R
import com.google.samples.apps.iosched.tv.ui.presenter.DetailsDescriptionPresenter
import com.google.samples.apps.iosched.tv.ui.presenter.SessionDetailsLogoPresenter
import com.google.samples.apps.iosched.shared.model.Session

/**
 * Displays the details for a [Session].
 */
class SessionDetailFragment : DetailsSupportFragment() {

    private lateinit var viewModel: SessionDetailViewModel
    // TODO: inject factory
    lateinit var viewModelFactory: SessionDetailViewModelFactory

    private lateinit var _adapter: ArrayObjectAdapter
    private lateinit var presenterSelector: ClassPresenterSelector
    private lateinit var detailsOverviewRow: DetailsOverviewRow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupAdapter()

        val dummySessionId = "61d80842-c8f6-e511-a517-00155d5066d7"

        // TODO: inject in view model factory
        val sessionRepository =
            SessionRepository(
                ConferenceDataRepository(
                    NetworkConferenceDataSource(
                        requireContext()
                    ), BootstrapConferenceDataSource
                )
            )
        viewModelFactory = SessionDetailViewModelFactory(sessionRepository)
        viewModel = viewModelProvider(viewModelFactory)
        viewModel.loadSessionById(dummySessionId)

        viewModel.session.observe(this, Observer { session ->
            // TODO: update presenters and not create a new instance and add it each time
            session?.let {
                detailsOverviewRow = DetailsOverviewRow(it)
                _adapter.add(detailsOverviewRow)
            }
        })
    }

    private fun setupAdapter() {
        val context = requireContext()

        // Set detail background and style.
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(
            DetailsDescriptionPresenter(),
            SessionDetailsLogoPresenter()
        ).apply {
            backgroundColor = ContextCompat.getColor(context, R.color.colorPrimaryDark)
            initialState = FullWidthDetailsOverviewRowPresenter.STATE_SMALL

            onActionClickedListener = OnActionClickedListener { action ->
                Toast.makeText(context, action.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        presenterSelector = ClassPresenterSelector().apply {
            addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
            addClassPresenter(ListRow::class.java, ListRowPresenter())
        }
        _adapter = ArrayObjectAdapter(presenterSelector)

        adapter = _adapter
    }
}
