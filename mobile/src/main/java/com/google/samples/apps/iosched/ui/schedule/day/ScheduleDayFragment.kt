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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnNextLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleDayBinding
import com.google.samples.apps.iosched.shared.result.EventObserver
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.shared.util.lazyFast
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

        fun newInstance(day: Int): ScheduleDayFragment {
            val args = Bundle().apply {
                putInt(ARG_CONFERENCE_DAY, day)
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

    private val conferenceDay: Int by lazyFast {
        val args = arguments ?: throw IllegalStateException("Missing arguments!")
        args.getInt(ARG_CONFERENCE_DAY)
    }

    private lateinit var adapter: ScheduleDayAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // VM shared across the [MainActivity], [ScheduleFragment] and the [ScheduleDayFragment]s.
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
            setRecycledViewPool(sessionViewPool)
            (layoutManager as LinearLayoutManager).recycleChildrenOnDetach = true
            (itemAnimator as DefaultItemAnimator).run {
                supportsChangeAnimations = false
                addDuration = 160L
                moveDuration = 160L
                changeDuration = 160L
                removeDuration = 120L
            }
        }

        // During conference, scroll to current event. Do this only once.
        viewModel.currentEvent.observe(this, Observer { eventLocation ->
            if (eventLocation != null &&
                !viewModel.userHasInteracted &&
                eventLocation.day == conferenceDay &&
                eventLocation.sessionIndex != -1
            ) {
                binding.recyclerview.run {
                    post {
                        (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                            eventLocation.sessionIndex,
                            resources.getDimensionPixelSize(R.dimen.margin_normal)
                        )
                    }
                }
            }
        })
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
            // TODO: Change once there's a way to show errors to the user
            Toast.makeText(this.context, errorMsg, Toast.LENGTH_LONG).show()
        })
    }

    private fun initializeList(sessionTimeData: SessionTimeData) {
        // Require the list and timeZoneId to be loaded.
        val list = sessionTimeData.list ?: return
        val timeZoneId = sessionTimeData.timeZoneId ?: return
        adapter.submitList(list)

        binding.recyclerview.run {
            // we want this to run after diffing
            doOnNextLayout {
                // Recreate the decoration used for the sticky time headers
                clearDecorations()
                if (list.isNotEmpty()) {
                    addItemDecoration(
                        ScheduleTimeHeadersDecoration(
                            it.context, list.map { it.session }, timeZoneId
                        )
                    )
                }
            }
        }

        binding.executeAfter {
            isEmpty = list.isEmpty()
        }
    }
}
