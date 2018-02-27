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

package com.google.samples.apps.iosched.ui.schedule

import android.arch.lifecycle.ViewModelProvider
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleBinding
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.ui.schedule.agenda.ScheduleAgendaFragment
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.fragment_schedule.*
import javax.inject.Inject

/**
 * The Schedule page of the top-level Activity.
 */
class ScheduleFragment : DaggerFragment() {

    companion object {
        val TAG: String = ScheduleFragment::class.java.simpleName
        val COUNT = ConferenceDay.values().size + 1 // Agenda
        val AGENDA_POSITION = COUNT - 1
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: ScheduleViewModel

    override fun onCreateView(inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        viewModel = activityViewModelProvider(viewModelFactory)
        val binding: FragmentScheduleBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_schedule, container, false)

        // Set the layout variables
        binding.viewModel = viewModel
        binding.setLifecycleOwner(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewpager.offscreenPageLimit = COUNT - 1
        viewpager.adapter = ScheduleAdapter(childFragmentManager)
        tabs.setupWithViewPager(viewpager)
    }

    /**
     * Adapter that build a page for each conference day.
     */
    inner class ScheduleAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getCount() = COUNT

        override fun getItem(position: Int): Fragment {
            return when(position) {
                AGENDA_POSITION -> ScheduleAgendaFragment()
                else -> ScheduleDayFragment.newInstance(ConferenceDay.values()[position])
            }
        }

        override fun getPageTitle(position: Int): CharSequence {
            return when(position) {
                AGENDA_POSITION -> getString(R.string.agenda)
                else -> ConferenceDay.values()[position].formatMonthDay()
            }
        }
    }
}
