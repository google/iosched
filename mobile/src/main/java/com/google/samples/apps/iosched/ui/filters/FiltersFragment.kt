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

package com.google.samples.apps.iosched.ui.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Button
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.databinding.BindingAdapter
import androidx.databinding.ObservableFloat
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentFiltersBinding
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.lerp
import com.google.samples.apps.iosched.util.slideOffsetToAlpha
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
class FiltersFragment : DaggerFragment() {

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
        // Threshold for when the filter list content reach maximum alpha. Should be a value between
        // 0 and [ALPHA_CHANGEOVER], inclusive.
        private const val ALPHA_CONTENT_END_ALPHA = 1f
        // Threshold for when the filter list content should starting changing alpha state
        // This should be a value between 0 and 1, coinciding with a point between the bottom
        // sheet's collapsed (0) and expanded (1) states.
        private const val ALPHA_CONTENT_START_ALPHA = 0.2f
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: FiltersViewModelDelegate

    private lateinit var filterAdapter: SelectableFilterChipAdapter

    private lateinit var binding: FragmentFiltersBinding

    private lateinit var behavior: BottomSheetBehavior<*>

    private var headerAlpha = ObservableFloat(1f)
    private var descriptionAlpha = ObservableFloat(1f)
    private var recyclerviewAlpha = ObservableFloat(1f)

    private val contentFadeInterpolator = LinearOutSlowInInterpolator()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFiltersBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            headerAlpha = this@FiltersFragment.headerAlpha
            descriptionAlpha = this@FiltersFragment.descriptionAlpha
            recyclerviewAlpha = this@FiltersFragment.recyclerviewAlpha
        }

        // Pad the bottom of the RecyclerView so that the content scrolls up above the nav bar
        binding.recyclerviewFilters.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePaddingRelative(bottom = padding.bottom + insets.systemWindowInsetBottom)
        }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // ViewModel is scoped to the parent fragment.
        // TODO(jdkoren) resolve FiltersViewModelDelegate
        // viewModel = parentViewModelProvider(viewModelFactory)
        binding.viewModel = viewModel

        behavior = BottomSheetBehavior.from(binding.filterSheet)

        filterAdapter = SelectableFilterChipAdapter(viewModel)
        viewModel.eventFilters.observe(viewLifecycleOwner, Observer {
            filterAdapter.submitEventFilterList(it)
        })

        binding.recyclerviewFilters.apply {
            adapter = filterAdapter
            setHasFixedSize(true)
            (layoutManager as GridLayoutManager).spanSizeLookup =
                EventFilterSpanSizeLookup(filterAdapter)
            addOnScrollListener(object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    binding.filtersHeaderShadow.isActivated = recyclerView.canScrollVertically(-1)
                }
            })
        }

        // Update the peek and margins so that it scrolls and rests within sys ui
        val peekHeight = behavior.peekHeight
        val marginBottom = binding.root.marginBottom
        binding.root.doOnApplyWindowInsets { v, insets, _ ->
            val gestureInsets = insets.systemGestureInsets
            // Update the peek height so that it is above the navigation bar
            behavior.peekHeight = gestureInsets.bottom + peekHeight

            v.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = marginBottom + insets.systemWindowInsetTop
            }
        }

        behavior.addBottomSheetCallback(object : BottomSheetCallback {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                updateFilterContentsAlpha(slideOffset)
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
            updateFilterContentsAlpha(slideOffset)
        }
    }

    private fun updateFilterContentsAlpha(slideOffset: Float) {
        if (viewModel.hasAnyFilters.value == true) {
            // If we have filter set, we will crossfade the header and description views.
            // Alpha of normal header views increases as the sheet expands, while alpha of
            // description views increases as the sheet collapses. To prevent overlap, we use
            // a threshold at which the views "trade places".
            headerAlpha.set(slideOffsetToAlpha(slideOffset, ALPHA_CHANGEOVER, ALPHA_HEADER_MAX))
            descriptionAlpha.set(slideOffsetToAlpha(slideOffset, ALPHA_CHANGEOVER, ALPHA_DESC_MAX))
        } else {
            // Otherwise we just show the header view
            headerAlpha.set(1f)
            descriptionAlpha.set(0f)
        }
        // Due to the content view being visible below the navigation bar, we apply a short alpha
        // transition
        recyclerviewAlpha.set(lerp(ALPHA_CONTENT_START_ALPHA, ALPHA_CONTENT_END_ALPHA,
                contentFadeInterpolator.getInterpolation(slideOffset)))
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

/**
 * Sets up the `onClickListener` for the filter reset button, so that it calls the given
 * [listener] with the side effect of animating deselecting any filters.
 */
@BindingAdapter(value = ["filterChips", "animatedOnClick"], requireAll = false)
fun setResetFiltersClickListener(
    reset: Button,
    filterChips: ViewGroup,
    listener: OnClickListener
) {
    reset.setOnClickListener {
        filterChips.forEach { child ->
            child.findViewById<FilterChipView>(R.id.filter_label)?.let { filterView ->
                if (filterView.isChecked) {
                    filterView.animateCheckedAndInvoke(false) {
                        listener.onClick(reset)
                    }
                }
            }
        }
    }
}

@BindingAdapter("filterChipOnClick", "viewModel", requireAll = true)
fun setClickListenerForFilterChip(
    view: FilterChipView,
    eventFilter: EventFilter,
    viewModel: FiltersViewModelDelegate
) {
    view.setOnClickListener {
        // TODO(jdkoren) restore sign in check if we need it later
        val checked = !view.isChecked
        view.animateCheckedAndInvoke(checked) {
            viewModel.toggleFilter(eventFilter, checked)
        }
    }
}

@BindingAdapter("filterChipText")
fun filterChipText(view: FilterChipView, filter: EventFilter) {
    val text = if (filter.getTextResId() != 0) {
        view.resources.getText(filter.getTextResId())
    } else {
        filter.getText()
    }
    view.text = text
}

@BindingAdapter("filterChipTextShort")
fun filterChipTextShort(view: FilterChipView, filter: EventFilter) {
    val text = if (filter.getShortTextResId() != 0) {
        view.resources.getText(filter.getShortTextResId())
    } else {
        filter.getShortText()
    }
    view.text = text
}
