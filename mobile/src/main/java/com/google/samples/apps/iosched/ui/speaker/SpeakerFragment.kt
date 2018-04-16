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

package com.google.samples.apps.iosched.ui.speaker

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.support.v7.widget.RecyclerView.RecycledViewPool
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.os.bundleOf
import com.google.samples.apps.iosched.databinding.FragmentSpeakerBinding
import com.google.samples.apps.iosched.shared.model.SpeakerId
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.prefs.SnackbarPreferenceViewModel
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailActivity
import com.google.samples.apps.iosched.ui.setUpSnackbar
import com.google.samples.apps.iosched.ui.signin.SignInDialogFragment
import com.google.samples.apps.iosched.util.postponeEnterTransition
import dagger.android.support.DaggerFragment
import javax.inject.Inject
import javax.inject.Named

/**
 * Fragment displaying speaker details and their events.
 */
class SpeakerFragment : DaggerFragment() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject lateinit var snackbarMessageManager: SnackbarMessageManager

    @Inject
    @field:Named("tagViewPool")
    lateinit var tagRecycledViewPool: RecycledViewPool

    private lateinit var speakerViewModel: SpeakerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        speakerViewModel = viewModelProvider(viewModelFactory)
        speakerViewModel.setSpeakerId(requireNotNull(arguments).getString(SPEAKER_ID))

        // Delay the Activity enter transition until speaker image has loaded
        activity?.postponeEnterTransition(500L)

        val binding = FragmentSpeakerBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@SpeakerFragment)
            viewModel = speakerViewModel
            tagViewPool = tagRecycledViewPool
            headshotLoadListener = object : ImageLoadListener {

                override fun onImageLoaded() {
                    activity?.startPostponedEnterTransition()
                }

                override fun onImageLoadFailed() {
                    activity?.startPostponedEnterTransition()
                }
            }
        }
        // If speaker does not have a profile image to load, we need to resume
        speakerViewModel.hasProfileImage.observe(this, Observer {
            if (it != true) {
                activity?.startPostponedEnterTransition()
            }
        })

        speakerViewModel.navigateToEventAction.observe(this, EventObserver { sessionId ->
            startActivity(SessionDetailActivity.starterIntent(requireContext(), sessionId))
        })

        speakerViewModel.navigateToSignInDialogAction.observe(this, EventObserver {
            val dialog = SignInDialogFragment()
            dialog.show(
                requireActivity().supportFragmentManager,
                SignInDialogFragment.DIALOG_NEED_TO_SIGN_IN
            )
        })

        val snackbarPrefViewModel: SnackbarPreferenceViewModel = viewModelProvider(viewModelFactory)
        setUpSnackbar(
            speakerViewModel.snackBarMessage,
            binding.snackbar,
            snackbarMessageManager,
            actionClickListener = {
                snackbarPrefViewModel.onStopClicked()
            }
        )

        binding.up.setOnClickListener {
            requireActivity().finishAfterTransition()
        }

        return binding.root
    }

    companion object {
        fun newInstance(speakerId: SpeakerId): SpeakerFragment {
            return SpeakerFragment().apply {
                arguments = bundleOf(SPEAKER_ID to speakerId)
            }
        }
    }
}
