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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.forEach
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentSessionDetailBinding
import com.google.samples.apps.iosched.model.Room
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.dialogs.SignInDialogDispatcher
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.prefs.SnackbarPreferenceViewModel
import com.google.samples.apps.iosched.ui.setUpSnackbar
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment
import com.google.samples.apps.iosched.ui.signin.NotificationsPreferenceDialogFragment.Companion.DIALOG_NOTIFICATIONS_PREFERENCE
import dagger.android.support.DaggerFragment
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class SessionDetailFragment : DaggerFragment() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject lateinit var snackbarMessageManager: SnackbarMessageManager

    private lateinit var sessionDetailViewModel: SessionDetailViewModel

    @Inject lateinit var analyticsHelper: AnalyticsHelper

    @Inject lateinit var signInDialogDispatcher: SignInDialogDispatcher

    @Inject
    @field:Named("tagViewPool")
    lateinit var tagRecycledViewPool: RecycledViewPool

    private var room: Room? = null

    lateinit var sessionTitle: String

    private val sessionId: String?
        get() = arguments?.let { SessionDetailFragmentArgs.fromBundle(it).sessionId }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sessionDetailViewModel = viewModelProvider(viewModelFactory)

        val binding = FragmentSessionDetailBinding.inflate(inflater, container, false).apply {
            viewModel = sessionDetailViewModel
            lifecycleOwner = this@SessionDetailFragment
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val detailsAdapter = SessionDetailAdapter(this, sessionDetailViewModel, tagRecycledViewPool)
        binding.sessionDetailRecyclerView.run {
            adapter = detailsAdapter
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
        }

        sessionDetailViewModel.session.observe(this, Observer {
            detailsAdapter.speakers = it?.speakers?.toList() ?: emptyList()
        })

        sessionDetailViewModel.relatedUserSessions.observe(this, Observer {
            detailsAdapter.related = it ?: emptyList()
        })

        sessionDetailViewModel.session.observe(this, Observer {
            room = it?.room
        })

        sessionDetailViewModel.navigateToYouTubeAction.observe(this, EventObserver { youtubeUrl ->
            openYoutubeUrl(youtubeUrl)
        })

        sessionDetailViewModel.navigateToSessionAction.observe(this, EventObserver { sessionId ->
            val action = SessionDetailFragmentDirections
                .actionSessionDetailFragmentSelf(sessionId)
            findNavController().navigate(action)
        })

        val snackbarPreferenceViewModel: SnackbarPreferenceViewModel =
            activityViewModelProvider(viewModelFactory)
        setUpSnackbar(
            sessionDetailViewModel.snackBarMessage,
            binding.snackbar,
            snackbarMessageManager,
            actionClickListener = {
                snackbarPreferenceViewModel.onStopClicked()
            }
        )

        sessionDetailViewModel.errorMessage.observe(this, EventObserver { errorMsg ->
            // TODO: Change once there's a way to show errors to the user
            Toast.makeText(this.context, errorMsg, Toast.LENGTH_LONG).show()
        })

        sessionDetailViewModel.navigateToSignInDialogAction.observe(this, EventObserver {
            signInDialogDispatcher.openSignInDialog(requireActivity())
        })

        sessionDetailViewModel.shouldShowNotificationsPrefAction.observe(this, EventObserver {
            if (it) {
                openNotificationsPreferenceDialog()
            }
        })

        sessionDetailViewModel.navigateToSpeakerDetail.observe(this, EventObserver { speakerId ->
            // TODO(jalc): Add shared transition here
            val action = SessionDetailFragmentDirections
                .actionSessionDetailFragmentToSpeakerFragment(speakerId)
            findNavController().navigate(action)

//            requireActivity().let {
//                val sharedElement =
//                    findSpeakerHeadshot(binding.sessionDetailRecyclerView, speakerId)
//                val options = ActivityOptions.makeSceneTransitionAnimation(
//                    it, sharedElement, it.getString(R.string.speaker_headshot_transition)
//                )
//                it.startActivity(SpeakerActivity.starterIntent(it, speakerId), options.toBundle())
//            }
        })

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        Timber.d("Loading details for session $arguments")

        sessionDetailViewModel.setSessionId(sessionId)
    }

    override fun onStop() {
        super.onStop()
        // Force a refresh when this screen gets added to a backstack and user comes back to it.
        sessionDetailViewModel.setSessionId(null)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        var titleUpdated = false
        sessionDetailViewModel.session.observe(this, Observer {
            if (it != null && !titleUpdated) {
                sessionTitle = it.title
                activity?.let { activity ->
                    analyticsHelper.sendScreenView("Session Details: $sessionTitle", activity)
                }
                titleUpdated = true
            }
        })
    }

    private fun openYoutubeUrl(youtubeUrl: String) {
        analyticsHelper.logUiEvent(sessionTitle, AnalyticsActions.YOUTUBE_LINK)
        startActivity(Intent(Intent.ACTION_VIEW, youtubeUrl.toUri()))
    }

    private fun openNotificationsPreferenceDialog() {
        val dialog = NotificationsPreferenceDialogFragment()
        dialog.show(requireActivity().supportFragmentManager, DIALOG_NOTIFICATIONS_PREFERENCE)
    }

    private fun findSpeakerHeadshot(speakers: ViewGroup, speakerId: SpeakerId): View {
        speakers.forEach {
            if (it.getTag(R.id.tag_speaker_id) == speakerId) {
                return it.findViewById(R.id.speaker_item_headshot)
            }
        }
        Timber.e("Could not find view for speaker id $speakerId")
        return speakers
    }
}
