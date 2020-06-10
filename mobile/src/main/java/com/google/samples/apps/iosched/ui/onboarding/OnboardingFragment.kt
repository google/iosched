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

package com.google.samples.apps.iosched.ui.onboarding

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import com.google.samples.apps.iosched.databinding.FragmentOnboardingBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint

private const val AUTO_ADVANCE_DELAY = 6_000L
private const val INITIAL_ADVANCE_DELAY = 3_000L

/**
 * Contains the pages of the onboarding experience and responds to [OnboardingViewModel] events.
 */
@AndroidEntryPoint
class OnboardingFragment : Fragment() {

    private val onboardingViewModel: OnboardingViewModel by viewModels()

    private lateinit var binding: FragmentOnboardingBinding

    private lateinit var pagerPager: ViewPagerPager

    private val handler = Handler()

    // Auto-advance the view pager to give overview of app benefits
    private val advancePager: Runnable = object : Runnable {
        override fun run() {
            pagerPager.advance()
            handler.postDelayed(this, AUTO_ADVANCE_DELAY)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false).apply {
            viewModel = onboardingViewModel
            lifecycleOwner = viewLifecycleOwner
            pager.adapter = OnboardingAdapter(childFragmentManager)
            pagerPager = ViewPagerPager(pager)
            // If user touches pager then stop auto advance
            pager.setOnTouchListener { _, _ ->
                handler.removeCallbacks(advancePager)
                false
            }
        }

        onboardingViewModel.navigateToMainActivity.observe(viewLifecycleOwner, EventObserver {
            requireActivity().run {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        })

        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        handler.postDelayed(advancePager, INITIAL_ADVANCE_DELAY)
    }

    override fun onDetach() {
        handler.removeCallbacks(advancePager)
        super.onDetach()
    }
}

class OnboardingAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

    // Don't show then countdown fragment if the conference has already started
    private val fragments = if (!TimeUtils.conferenceHasStarted()) {
        // Before the conference
        arrayOf(
            WelcomePreConferenceFragment(),
            OnboardingSignInFragment()
        )
    } else if (TimeUtils.conferenceHasStarted() && !TimeUtils.conferenceHasEnded()) {
        // During the conference
        arrayOf(
            WelcomeDuringConferenceFragment(),
            OnboardingSignInFragment()
        )
    } else {
        // Post the conference
        arrayOf(
            WelcomePostConferenceFragment()
        )
    }

    override fun getItem(position: Int) = fragments[position]

    override fun getCount() = fragments.size
}
