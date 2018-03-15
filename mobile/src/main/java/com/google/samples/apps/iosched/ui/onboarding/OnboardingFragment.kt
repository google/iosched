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
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.samples.apps.iosched.databinding.FragmentOnboardingBinding
import com.google.samples.apps.iosched.shared.util.TimeUtils

private const val AUTO_ADVANCE_DELAY = 6000L
private const val INITIAL_ADVANCE_DELAY = 3000L

class OnboardingFragment : Fragment() {

    private val handler = Handler()

    // Auto-advance the view pager to give overview of app benefits
    private val advancePager: Runnable = object : Runnable {
        override fun run() {
            binding.pager.run { currentItem = ((currentItem + 1) % adapter!!.count) }
            handler.postDelayed(this, AUTO_ADVANCE_DELAY)
        }
    }

    private lateinit var binding: FragmentOnboardingBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@OnboardingFragment)
            pager.adapter = OnboardingAdapter(childFragmentManager)
            // If user touches pager then stop auto advance
            pager.setOnTouchListener { _, _ ->
                handler.removeCallbacks(advancePager)
                false
            }
            pageIndicator.setViewPager(pager)
        }
        return binding.root
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        handler.postDelayed(advancePager, INITIAL_ADVANCE_DELAY)
    }

    override fun onDetach() {
        super.onDetach()
        handler.removeCallbacks(advancePager)
    }
}

class OnboardingAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {

    // Don't show then countdown fragment if the conference has already started
    private val fragments = if (TimeUtils.conferenceHasStarted()) {
        arrayOf(
            WelcomeFragment(),
            CustomizeScheduleFragment()
        )
    } else {
        arrayOf(
            WelcomeFragment(),
            CustomizeScheduleFragment(),
            CountdownFragment()
        )
    }

    override fun getItem(position: Int) = fragments[position]

    override fun getCount() = fragments.size

}
