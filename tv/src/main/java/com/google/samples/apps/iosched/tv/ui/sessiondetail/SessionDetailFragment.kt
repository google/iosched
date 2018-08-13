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

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.DetailsSupportFragment
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.DetailsOverviewRow
import androidx.leanback.widget.DiffCallback
import androidx.leanback.widget.FullWidthDetailsOverviewRowPresenter
import androidx.leanback.widget.FullWidthDetailsOverviewSharedElementHelper
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnActionClickedListener
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.Speaker
import com.google.samples.apps.iosched.shared.util.SpeakerUtils
import com.google.samples.apps.iosched.shared.util.getThemeColor
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.tv.R
import com.google.samples.apps.iosched.tv.app
import com.google.samples.apps.iosched.tv.ui.presenter.DetailsDescriptionPresenter
import com.google.samples.apps.iosched.tv.ui.presenter.SessionDetailsLogoPresenter
import com.google.samples.apps.iosched.tv.ui.presenter.SpeakerPresenter
import com.google.samples.apps.iosched.tv.ui.sessionplayer.SessionPlayerActivity
import com.google.samples.apps.iosched.tv.util.toArrayObjectAdapter
import javax.inject.Inject

private const val ACTION_WATCH = 1L
//private const val ACTION_STAR = 2L

/**
 * Displays the details for a [Session].
 */
class SessionDetailFragment : DetailsSupportFragment() {

    private lateinit var viewModel: SessionDetailViewModel
    @Inject lateinit var viewModelFactory: SessionDetailViewModelFactory

    // Backing adapter for DetailsSupportFragment
    private lateinit var _adapter: ArrayObjectAdapter
    private lateinit var presenterSelector: ClassPresenterSelector
    private lateinit var detailsPresenter: FullWidthDetailsOverviewRowPresenter
    private var detailsOverviewRow: DetailsOverviewRow? = null
    private lateinit var speakerListRow: ListRow
    private val speakerAdapter = ArrayObjectAdapter(SpeakerPresenter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app().sessionDetailComponent.inject(sessionDetailFragment = this)

        setupAdapter()
        val speakerHeader = HeaderItem(getString(R.string.session_detail_speakers_header))
        speakerListRow = ListRow(speakerHeader, speakerAdapter)

        val sessionId = requireActivity().intent.extras
            .getString(SessionDetailActivity.EXTRA_SESSION_ID)

        viewModel = viewModelProvider(viewModelFactory)
        viewModel.loadSessionById(sessionId)

        viewModel.session.observe(this, Observer { session ->
            session?.let {
                if (detailsOverviewRow == null) {
                    detailsOverviewRow = DetailsOverviewRow(it).apply {
                        actionsAdapter = createSessionActions(it)
                        detailsPresenter.onActionClickedListener =
                                SessionDetailsOnActionClickedListener(requireContext(), it)

                        _adapter.add(this)
                        if (it.speakers.isNotEmpty()) {
                            updateSpeakers(it)
                            _adapter.add(speakerListRow)
                        }
                    }
                } else {
                    detailsOverviewRow?.item = it
                    updateSpeakers(it)
                }
            }
        })
    }

    private fun createSessionActions(session: Session): ArrayObjectAdapter {
        val actions = mutableListOf<Action>()
        if (session.hasVideo()) {
            actions += Action(
                ACTION_WATCH,
                resources.getString(R.string.session_detail_watch),
                null,
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_livestreamed)
            )
        }
        // TODO: add conditionally based on user's prefs.
//        actions += Action(
//            ACTION_STAR,
//            resources.getString(R.string.session_detail_star),
//            null,
//            // TODO: change icon based on user state.
//            ContextCompat.getDrawable(requireContext(), R.drawable.ic_star_border)
//        )
        return actions.toArrayObjectAdapter()
    }

    private fun updateSpeakers(session: Session) {

        val speakers = SpeakerUtils.alphabeticallyOrderedSpeakerList(session.speakers)
        speakerAdapter.setItems(speakers, object : DiffCallback<Speaker>() {
            override fun areItemsTheSame(oldItem: Speaker, newItem: Speaker): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Speaker, newItem: Speaker): Boolean {
                return oldItem == newItem
            }
        })
    }

    private fun setupAdapter() {
        val context = requireContext()

        // Set detail background and style.
        detailsPresenter = FullWidthDetailsOverviewRowPresenter(
            DetailsDescriptionPresenter(),
            SessionDetailsLogoPresenter()
        ).apply {
            backgroundColor = context.getThemeColor(R.attr.colorPrimaryDark, R.color.indigo_dark)
            initialState = FullWidthDetailsOverviewRowPresenter.STATE_SMALL
        }

        presenterSelector = ClassPresenterSelector().apply {
            addClassPresenter(DetailsOverviewRow::class.java, detailsPresenter)
            addClassPresenter(ListRow::class.java, ListRowPresenter())
        }
        _adapter = ArrayObjectAdapter(presenterSelector)

        adapter = _adapter
    }
}

/**
 * Handles selected actions from the detail's presenter.
 */
private class SessionDetailsOnActionClickedListener(
    private val context: Context,
    private val session: Session
) : OnActionClickedListener {

    override fun onActionClicked(action: Action) {
        if (action.id == ACTION_WATCH && session.hasVideo()) {
            val playerIntent = SessionPlayerActivity.createIntent(context, session.id)
            context.startActivity(playerIntent)
        } else {
            // TODO: Remove toast once all actions are implemented
            Toast.makeText(context, action.toString(), Toast.LENGTH_SHORT)
                .show()
        }
    }
}
