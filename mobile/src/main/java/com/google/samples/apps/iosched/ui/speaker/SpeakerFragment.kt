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

import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnNextLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.transition.TransitionInflater
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentSpeakerBinding
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.ScheduleTwoPaneViewModel
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.launchAndRepeatWithViewLifecycle
import com.google.samples.apps.iosched.util.setContentMaxWidth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * Fragment displaying speaker details and their events.
 */
@AndroidEntryPoint
class SpeakerFragment : Fragment(), OnOffsetChangedListener {

    @Inject lateinit var snackbarMessageManager: SnackbarMessageManager

    @Inject lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    @field:Named("tagViewPool")
    lateinit var tagRecycledViewPool: RecycledViewPool

    private val speakerViewModel: SpeakerViewModel by viewModels()
    private val scheduleTwoPaneViewModel: ScheduleTwoPaneViewModel by activityViewModels()

    private lateinit var binding: FragmentSpeakerBinding

    private var toolbarCollapsed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        sharedElementEnterTransition =
            TransitionInflater.from(context).inflateTransition(R.transition.speaker_shared_enter)
        // Delay the enter transition until speaker image has loaded.
        postponeEnterTransition(500L, TimeUnit.MILLISECONDS)

        val imageLoadListener = object : ImageLoadListener {
            override fun onImageLoaded() {
                startPostponedEnterTransition()
            }

            override fun onImageLoadFailed() {
                startPostponedEnterTransition()
            }
        }

        val themedInflater =
            inflater.cloneInContext(ContextThemeWrapper(requireActivity(), R.style.AppTheme_Detail))
        binding = FragmentSpeakerBinding.inflate(themedInflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            headshotLoadListener = imageLoadListener
            viewModel = speakerViewModel
        }

        val speakerAdapter = SpeakerAdapter(
            viewLifecycleOwner,
            speakerViewModel,
            tagRecycledViewPool,
            scheduleTwoPaneViewModel
        )
        binding.speakerDetailRecyclerView.run {
            adapter = speakerAdapter
            itemAnimator?.run {
                addDuration = 120L
                moveDuration = 120L
                changeDuration = 120L
                removeDuration = 100L
            }
            doOnApplyWindowInsets { view, insets, padding ->
                val systemInsets = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
                )
                view.updatePadding(bottom = padding.bottom + systemInsets.bottom)
                // CollapsingToolbarLayout's default scrim visible trigger height is a bit large.
                // Choose something smaller so that the content stays visible longer.
                binding.collapsingToolbar.scrimVisibleHeightTrigger = systemInsets.top * 2
            }
            doOnNextLayout {
                setContentMaxWidth(this)
            }
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchAndRepeatWithViewLifecycle {
            launch {
                speakerViewModel.speaker.collect {
                    if (it != null) {
                        val pageName = "Speaker Details: ${it.name}"
                        analyticsHelper.sendScreenView(pageName, requireActivity())
                    }
                }
            }

            launch {
                speakerViewModel.speakerUserSessions.collect {
                    (binding.speakerDetailRecyclerView.adapter as SpeakerAdapter)
                        .speakerSessions = it
                }
            }

            // If speaker does not have a profile image to load, we need to resume.
            launch {
                speakerViewModel.hasNoProfileImage.collect {
                    if (it) {
                        startPostponedEnterTransition()
                    }
                }
            }
        }
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        val collapsed = (-verticalOffset >= appBarLayout.totalScrollRange)
        if (collapsed != toolbarCollapsed) {
            toolbarCollapsed = collapsed
            // We have transparent status bar, so we don't use CollapsingToolbarLayout's
            // statusBarScrim. Instead fade out the views when collapsed.
            val alpha = if (collapsed) 0f else 1f
            binding.toolbar.animate().alpha(alpha).start()
            binding.speakerImage.animate().alpha(alpha).start()
        }
    }
}
