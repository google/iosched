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

package com.google.samples.apps.iosched.ui.schedule.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableFloat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleFilterBinding
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import com.google.samples.apps.iosched.ui.schedule.filters.EventFilter.MyEventsFilter
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_COLLAPSED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_EXPANDED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_HIDDEN
import com.google.samples.apps.iosched.widget.SpaceDecoration
import dagger.android.support.DaggerFragment
import javax.inject.Inject

/**
 * Fragment that shows the list of filters for the Schedule
 */
class ScheduleFilterFragment : DaggerFragment() {

    companion object {
        // Threshold for when normal header views and description views should "change places".
        // This should be a value between 0 and 1, coinciding with a point between the bottom
        // sheet's collapsed (0) and expanded (1) states.
        private const val ALPHA_CHANGEOVER = 0.33f
        // Threshold for when description views reach maximum alpha. Should be a value between
        // 0 and [ALPHA_CHANGEOVER], inclusive.
        private const val ALPHA_DESC_MAX = 0f
        // Threshold for when normal header views reach maximum alpha. Should be a value between
        // [ALPHA_CHANGEOVER] and 1, inclusive.
        private const val ALPHA_HEADER_MAX = 0.67f
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: ScheduleViewModel

    private lateinit var filterAdapter: ScheduleFilterAdapter

    private lateinit var binding: FragmentScheduleFilterBinding

    private lateinit var behavior: BottomSheetBehavior<*>

    private var headerAlpha = ObservableFloat(1f)
    private var descriptionAlpha = ObservableFloat(1f)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentScheduleFilterBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@ScheduleFilterFragment)
            headerAlpha = this@ScheduleFilterFragment.headerAlpha
            descriptionAlpha = this@ScheduleFilterFragment.descriptionAlpha
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = activityViewModelProvider(viewModelFactory)
        binding.viewModel = viewModel

        behavior = BottomSheetBehavior.from(binding.filterSheet)

        filterAdapter = ScheduleFilterAdapter(viewModel)
        viewModel.eventFilters.observe(this, Observer { filterAdapter.submitEventFilterList(it) })

        binding.recyclerview.apply {
            adapter = filterAdapter
            setHasFixedSize(true)
            (layoutManager as GridLayoutManager).spanSizeLookup =
                ScheduleFilterSpanSizeLookup(filterAdapter)
            addOnScrollListener(object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    binding.filtersHeaderShadow.isActivated = recyclerView.canScrollVertically(-1)
                }
            })
        }

        behavior.addBottomSheetCallback(object : BottomSheetCallback {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                updateFilterHeadersAlpha(slideOffset)
            }
        })

        binding.collapseArrow.setOnClickListener {
            behavior.state = if (behavior.skipCollapsed) STATE_HIDDEN else STATE_COLLAPSED
        }

        binding.expand.setOnClickListener {
            behavior.state = STATE_EXPANDED
        }

        // This fragment is in the layout of a parent fragment, so its view hierarchy is restored
        // when the parent's hierarchy is restored. However, the dispatch order seems to traverse
        // child fragments first, meaning the views we care about have not actually been restored
        // when onViewStateRestored is called (otherwise we would do this there).
        binding.filterSheet.doOnLayout {
            val slideOffset = when (behavior.state) {
                STATE_EXPANDED -> 1f
                STATE_COLLAPSED -> 0f
                else /*BottomSheetBehavior.STATE_HIDDEN*/ -> -1f
            }
            updateFilterHeadersAlpha(slideOffset)
        }
    }

    private fun updateFilterHeadersAlpha(slideOffset: Float) {
        // Alpha of normal header views increases as the sheet expands, while alpha of description
        // views increases as the sheet collapses. To prevent overlap, we use a threshold at which
        // the views "trade places".
        headerAlpha.set(offsetToAlpha(slideOffset, ALPHA_CHANGEOVER, ALPHA_HEADER_MAX))
        descriptionAlpha.set(offsetToAlpha(slideOffset, ALPHA_CHANGEOVER, ALPHA_DESC_MAX))
    }

    /**
     * Map a slideOffset (in the range `[-1, 1]`) to an alpha value based on the desired range.
     * For example, `offsetToAlpha(0.5, 0.25, 1) = 0.33` because 0.5 is 1/3 of the way between 0.25
     * and 1. The result value is additionally clamped to the range `[0, 1]`.
     */
    private fun offsetToAlpha(value: Float, rangeMin: Float, rangeMax: Float): Float {
        return ((value - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)
    }
}

@BindingAdapter("selectedFilters")
fun selectedFilters(recyclerView: RecyclerView, filters: List<EventFilter>?) {
    val filterChipAdapter: FilterChipAdapter
    if (recyclerView.adapter == null) {
        filterChipAdapter = FilterChipAdapter()
        recyclerView.apply {
            adapter = filterChipAdapter
            addItemDecoration(
                SpaceDecoration(
                    end = resources.getDimensionPixelSize(R.dimen.spacing_normal)
                )
            )
        }
    } else {
        filterChipAdapter = recyclerView.adapter as FilterChipAdapter
    }
    filterChipAdapter.filters = filters ?: emptyList()
    filterChipAdapter.notifyDataSetChanged()
}

@BindingAdapter(value = ["hasFilters", "eventCount"], requireAll = true)
fun filterHeader(textView: TextView, hasFilters: Boolean?, eventCount: Int?) {
    if (hasFilters == true && eventCount != null) {
        textView.text = textView.resources.getQuantityString(
            R.plurals.filter_event_count, eventCount, eventCount
        )
    } else {
        textView.setText(R.string.filters)
    }
}

@BindingAdapter("eventFilter", "viewModel", requireAll = true)
fun setClickListenerForFilter(
    filter: EventFilterView,
    eventFilter: EventFilter,
    viewModel: ScheduleViewModel
) {
    filter.setOnClickListener {
        if (eventFilter is MyEventsFilter && !viewModel.isSignedIn()) {
            viewModel.onSignInRequired()
        } else {
            val checked = !filter.isChecked
            filter.animateCheckedAndInvoke(checked) {
                viewModel.toggleFilter(eventFilter, checked)
            }
        }
    }
}

/**
 * Sets up the `onClickListener` for the filter reset button, so that it calls the given
 * [listener] with the side effect of animating deselecting any filters.
 */
@BindingAdapter(value = ["eventFilters", "animatedOnClick"], requireAll = false)
fun setResetFiltersClickListener(
    reset: Button,
    eventFilters: ViewGroup,
    listener: OnClickListener
) {
    reset.setOnClickListener {
        eventFilters.forEach { outer ->
            if (outer is ViewGroup) {
                outer.forEach { view ->
                    if (view is EventFilterView && view.isChecked) {
                        view.animateCheckedAndInvoke(false) {
                            listener.onClick(reset)
                        }
                    }
                }
            }
        }
    }
}

@BindingAdapter("eventFilterText")
fun eventFilterText(view: EventFilterView, filter: EventFilter) {
    val text = if (filter.getTextResId() != 0) {
        view.resources.getText(filter.getTextResId())
    } else {
        filter.getText()
    }
    view.text = text
}

@BindingAdapter("eventFilterTextShort")
fun eventFilterTextShort(view: EventFilterView, filter: EventFilter) {
    val text = if (filter.getShortTextResId() != 0) {
        view.resources.getText(filter.getShortTextResId())
    } else {
        filter.getShortText()
    }
    view.text = text
}
