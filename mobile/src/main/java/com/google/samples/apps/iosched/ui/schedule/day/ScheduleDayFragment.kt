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

package com.google.samples.apps.iosched.ui.schedule.day

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView.RecycledViewPool
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.samples.apps.iosched.databinding.FragmentScheduleDayBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.shared.util.getEnum
import com.google.samples.apps.iosched.shared.util.lazyFast
import com.google.samples.apps.iosched.shared.util.putEnum
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import com.google.samples.apps.iosched.ui.schedule.SessionTimeData
import com.google.samples.apps.iosched.util.clearDecorations
import com.google.samples.apps.iosched.util.executeAfter
import dagger.android.support.DaggerFragment
import javax.inject.Inject
import javax.inject.Named

/**
 * Fragment displaying a single conference day's schedule
 */
class ScheduleDayFragment : DaggerFragment() {

    companion object {
        private const val TAG = "ScheduleDayFragment"
        private const val ARG_CONFERENCE_DAY = "arg.CONFERENCE_DAY"

        fun newInstance(day: ConferenceDay): ScheduleDayFragment {
            val args = Bundle().apply {
                putEnum(ARG_CONFERENCE_DAY, day)
            }
            return ScheduleDayFragment().apply { arguments = args }
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: ScheduleViewModel
    private lateinit var binding: FragmentScheduleDayBinding

    @Inject
    @field:Named("sessionViewPool")
    lateinit var sessionViewPool: RecycledViewPool

    @Inject
    @field:Named("tagViewPool")
    lateinit var tagViewPool: RecycledViewPool

    private val conferenceDay: ConferenceDay by lazyFast {
        val args = arguments ?: throw IllegalStateException("Missing arguments!")
        args.getEnum<ConferenceDay>(ARG_CONFERENCE_DAY)
    }

    private lateinit var adapter: ScheduleDayAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = activityViewModelProvider(viewModelFactory)
        binding = FragmentScheduleDayBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@ScheduleDayFragment)
            viewModel = this@ScheduleDayFragment.viewModel
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ScheduleDayAdapter(
            viewModel,
            tagViewPool,
            viewModel.showReservations,
            viewModel.timeZoneId,
            this
        )

        binding.recyclerview.apply {
            adapter = this@ScheduleDayFragment.adapter
            recycledViewPool = sessionViewPool
            (layoutManager as LinearLayoutManager).recycleChildrenOnDetach = true
            (itemAnimator as DefaultItemAnimator).supportsChangeAnimations = false
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val activity = requireActivity()
        viewModel.getSessionTimeDataForDay(conferenceDay).observe(activity, Observer {
            it ?: return@Observer
            initializeList(it)
        })

        // Show an error message
        viewModel.errorMessage.observe(this, EventObserver { errorMsg ->
            //TODO: Change once there's a way to show errors to the user
            Toast.makeText(this.context, errorMsg, Toast.LENGTH_LONG).show()
        })
    }

    fun initializeList(sessionTimeData: SessionTimeData) {
        // Require the list and timeZoneId to be loaded.
        val list = sessionTimeData.list ?: return
        val timeZoneId = sessionTimeData.timeZoneId ?: return
        adapter.submitList(list)

        binding.recyclerview.let {
            // Recreate the decoration used for the sticky time headers
            it.clearDecorations()
            if (list.isNotEmpty()) {
                it.addItemDecoration(
                    ScheduleTimeHeadersDecoration(it.context, list.map { it.session }, timeZoneId)
                )
            }
        }

        binding.executeAfter {
            isEmpty = list.isEmpty()
        }
    }
}


