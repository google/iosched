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

package com.google.samples.apps.iosched.ui.sessiondetail

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.ShareCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.net.toUri
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentSessionDetailBinding
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class SessionDetailFragment : DaggerFragment() {

    private var shareString = ""

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)

        val sessionDetailViewModel: SessionDetailViewModel = viewModelProvider(viewModelFactory)
        sessionDetailViewModel.loadSessionById(checkNotNull(arguments).getString(EXTRA_SESSION_ID))

        val binding: FragmentSessionDetailBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_session_detail, container, false
        )
        binding.run {
            viewModel = sessionDetailViewModel
            setLifecycleOwner(this@SessionDetailFragment)
            toolbar.inflateMenu(R.menu.session_detail_menu)
            // todo setup menu & fab based on attendee
            toolbar.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.menu_item_share) {
                    ShareCompat.IntentBuilder.from(activity)
                        .setType("text/plain")
                        .setText(shareString)
                        .setChooserTitle(R.string.intent_chooser_session_detail)
                        .startChooser()
                }
                true
            }
            up.setOnClickListener {
                requireActivity().finishAfterTransition()
            }
        }

        sessionDetailViewModel.session.observe(this, Observer {
            shareString = if (it == null) {
                ""
            } else {
                getString(R.string.share_text_session_detail, it.title, it.sessionUrl)
            }
        })

        sessionDetailViewModel.navigateToYouTubeAction.observe(this, Observer { navigationEvent ->
            navigationEvent?.getContentIfNotHandled()?.let { youtubeUrl ->
                openYoutubeUrl(youtubeUrl)
            }
        })

        return binding.root
    }

    private fun openYoutubeUrl(youtubeUrl: String) {
        startActivity(Intent(Intent.ACTION_VIEW, youtubeUrl.toUri()))
    }

    companion object {
        private const val EXTRA_SESSION_ID = "SESSION_ID"

        fun newInstance(sessionId: String): SessionDetailFragment {
            val bundle = Bundle().apply { putString(EXTRA_SESSION_ID, sessionId) }
            return SessionDetailFragment().apply { arguments = bundle }
        }
    }
}
