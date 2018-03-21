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
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v17.leanback.app.DetailsSupportFragment
import android.support.v17.leanback.widget.Action
import android.support.v17.leanback.widget.ArrayObjectAdapter
import android.support.v17.leanback.widget.ClassPresenterSelector
import android.support.v17.leanback.widget.DetailsOverviewRow
import android.support.v17.leanback.widget.DiffCallback
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper
import android.support.v17.leanback.widget.HeaderItem
import android.support.v17.leanback.widget.ListRow
import android.support.v17.leanback.widget.ListRowPresenter
import android.support.v17.leanback.widget.OnActionClickedListener
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.util.SpeakerUtils
import com.google.samples.apps.iosched.shared.util.getThemeColor
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.tv.R
import com.google.samples.apps.iosched.tv.app
import com.google.samples.apps.iosched.tv.ui.presenter.DetailsDescriptionPresenter
import com.google.samples.apps.iosched.tv.ui.presenter.SessionDetailsLogoPresenter
import com.google.samples.apps.iosched.tv.ui.presenter.SessionPresenter
import com.google.samples.apps.iosched.tv.ui.presenter.SpeakerPresenter
import com.google.samples.apps.iosched.tv.util.toArrayObjectAdapter
import javax.inject.Inject

private const val ACTION_WATCH = 1L
private const val ACTION_BOOKMARK = 2L

/**
 * Displays the details for a [Session].
 */
class SessionDetailFragment : DetailsSupportFragment() {

    private lateinit var viewModel: SessionDetailViewModel
    @Inject lateinit var viewModelFactory: SessionDetailViewModelFactory

    // Backing adapter for DetailsSupportFragment
    private lateinit var _adapter: ArrayObjectAdapter
    private lateinit var presenterSelector: ClassPresenterSelector
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

                        actionsAdapter = createSessionActions()

                        _adapter.add(this)
                        updateSpeakers(it)
                        _adapter.add(speakerListRow)
                    }
                } else {
                    detailsOverviewRow?.item = it
                    updateSpeakers(it)
                }
                updateLogoImage(it)
            }
        })
    }

    private fun createSessionActions(): ArrayObjectAdapter {
        //TODO: handle when actions are selected.
        val actionWatch = Action(
            ACTION_WATCH,
            resources.getString(R.string.session_detail_watch),
            null,
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_livestreamed)
        )

        val actionBookmark = Action(
            ACTION_BOOKMARK,
            resources.getString(R.string.session_detail_star),
            null,
            //TODO: change icon based on user state.
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_star_border)
        )
        return listOf(actionWatch, actionBookmark).toArrayObjectAdapter()
    }

    private fun updateLogoImage(session: Session) {
        val options = RequestOptions()
            .error(R.drawable.default_background)
            .dontAnimate()

        Glide.with(requireContext())
            .asBitmap()
            .load(session.photoUrl)
            .apply(options)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    detailsOverviewRow?.setImageBitmap(requireContext(), resource)
                    startEntranceTransition()
                }
            })
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

        // Use helper to handle transitions of the logo image.
        val transitionHelper = FullWidthDetailsOverviewSharedElementHelper()
        transitionHelper.setSharedElementEnterTransition(
            activity,
            getString(R.string.shared_element_logo_name)
        )

        // Set detail background and style.
        val detailsPresenter = FullWidthDetailsOverviewRowPresenter(
            DetailsDescriptionPresenter(),
            SessionDetailsLogoPresenter()
        ).apply {
            backgroundColor = context.getThemeColor(R.attr.colorPrimaryDark, R.color.indigo_dark)
            initialState = FullWidthDetailsOverviewRowPresenter.STATE_SMALL
            // Prepare transition from the grid view.
            setListener(transitionHelper)
            isParticipatingEntranceTransition = false
            prepareEntranceTransition()

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
