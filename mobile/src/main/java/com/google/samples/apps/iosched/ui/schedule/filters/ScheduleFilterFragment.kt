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

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.databinding.BindingAdapter
import android.databinding.ObservableFloat
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.OnScrollListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.view.doOnLayout
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentScheduleFilterBinding
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher.PinnedEventMatcher
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher.TagFilterMatcher
import com.google.samples.apps.iosched.shared.util.activityViewModelProvider
import com.google.samples.apps.iosched.ui.MainActivity
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_COLLAPSED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_EXPANDED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_HIDDEN
import com.google.samples.apps.iosched.widget.HideBottomViewOnScrollBehavior
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
        viewModel.tagFilters.observe(this, Observer { filterAdapter.submitTagFilterList(it) })

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

        behavior.setBottomSheetCallback(object : BottomSheetCallback {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val activity = requireActivity() as MainActivity
                // Lock the bottom navigation hidden while the filters sheet is expanded.
                when (newState) {
                    STATE_EXPANDED -> activity.setBottomNavLockMode(
                        HideBottomViewOnScrollBehavior.LOCK_MODE_LOCKED_HIDDEN
                    )
                    STATE_COLLAPSED, STATE_HIDDEN -> activity.setBottomNavLockMode(
                        HideBottomViewOnScrollBehavior.LOCK_MODE_UNLOCKED
                    )
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                updateFilterHeadersAlpha(slideOffset)
            }
        })

        // We can't use DataBinding on <fragment> tags, so set up an observer manually.
        viewModel.hasAnyFilters.observe(this, Observer {
            val hasFilters = it ?: false
            behavior.skipCollapsed = !hasFilters
            behavior.isHideable = !hasFilters
            if (!hasFilters && behavior.state == STATE_COLLAPSED) {
                behavior.state = STATE_HIDDEN
            }
        })

        viewModel.userSessionMatcher.observe(this, Observer {
            val matcher = it ?: return@Observer
            bindDraggableState(matcher)
            setDescriptionView(matcher)
        })

        binding.collapseArrow.setOnClickListener {
            behavior.state = if (behavior.skipCollapsed) STATE_HIDDEN else STATE_COLLAPSED
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

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
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

    private fun bindDraggableState(matcher: UserSessionMatcher) {
        when (matcher) {
            is TagFilterMatcher -> behavior.isDraggable = true
            PinnedEventMatcher -> {
                behavior.isDraggable = false
                if (behavior.state == STATE_EXPANDED) {
                    behavior.state = STATE_COLLAPSED
                }
            }
        }
    }

    private fun setDescriptionView(matcher: UserSessionMatcher) {
        binding.filterDescriptionTags.visibility =
                if (matcher is TagFilterMatcher) View.VISIBLE else View.GONE
        binding.filterDescriptionPinned.visibility =
                if (matcher == PinnedEventMatcher) View.VISIBLE else View.GONE
    }
}

@BindingAdapter("filterDescriptionTags")
fun filterDescriptionTags(recyclerView : RecyclerView, tags: List<Tag>?) {
    val tagChipAdapter: TagChipAdapter
    if (recyclerView.adapter == null) {
        tagChipAdapter = TagChipAdapter()
        recyclerView.apply {
            adapter = tagChipAdapter
            addItemDecoration(SpaceDecoration(
                end = resources.getDimensionPixelSize(R.dimen.spacing_normal)
            ))
        }
    } else {
        tagChipAdapter = recyclerView.adapter as TagChipAdapter
    }
    tagChipAdapter.tags = tags ?: emptyList()
    tagChipAdapter.notifyDataSetChanged()
}

@BindingAdapter("clearFilterShortcutIcon")
fun clearFilterShortcutIcon(
    view: ImageView,
    matcher: UserSessionMatcher
) {
    when (matcher) {
        PinnedEventMatcher -> view.apply {
            setImageResource(R.drawable.ic_undo)
            contentDescription = resources.getString(R.string.a11y_revert_pinned)
        }
        is TagFilterMatcher -> view.apply {
            setImageResource(R.drawable.ic_clear_all)
            contentDescription = resources.getString(R.string.a11y_clear_tag_filters)
        }
    }
}

@BindingAdapter("clearFilterShortcutClick")
fun clearFilterShortcutClick(view: View, viewModel: ScheduleViewModel) {
    view.setOnClickListener {
        when (viewModel.userSessionMatcher.value){
            PinnedEventMatcher -> viewModel.togglePinnedEvents(false)
            is TagFilterMatcher -> viewModel.clearTagFilters()
        }
    }
}
