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
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnLayout
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.databinding.ObservableFloat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.google.android.flexbox.FlexboxItemDecoration
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentFiltersBinding
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.slideOffsetToAlpha
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_COLLAPSED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_EXPANDED
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.Companion.STATE_HIDDEN

/**
 * Fragment that shows the list of filters for the Schedule
 */
abstract class FiltersFragment : Fragment() {

    companion object {
        // Threshold for when the filter sheet content should become invisible.
        // This should be a value between 0 and 1, coinciding with a point between the bottom
        // sheet's collapsed (0) and expanded (1) states.
        private const val ALPHA_CONTENT_START = 0.1f
        // Threshold for when the filter sheet content should become visible.
        // This should be a value between 0 and 1, coinciding with a point between the bottom
        // sheet's collapsed (0) and expanded (1) states.
        private const val ALPHA_CONTENT_END = 0.3f
    }

    private lateinit var viewModel: FiltersViewModelDelegate

    private lateinit var filterAdapter: SelectableFilterChipAdapter

    private lateinit var binding: FragmentFiltersBinding

    private lateinit var behavior: BottomSheetBehavior<*>

    private var contentAlpha = ObservableFloat(1f)

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (::behavior.isInitialized && behavior.state == STATE_EXPANDED) {
                behavior.state = STATE_HIDDEN
            }
        }
    }

    private var pendingSheetState = -1

    /** Resolve the [FiltersViewModelDelegate] for this instance. */
    abstract fun resolveViewModelDelegate(): FiltersViewModelDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFiltersBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            contentAlpha = this@FiltersFragment.contentAlpha
        }

        // Pad the bottom of the RecyclerView so that the content scrolls up above the nav bar
        binding.recyclerviewFilters.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePaddingRelative(bottom = padding.bottom + insets.systemWindowInsetBottom)
        }

        return binding.root
    }

    // In order to acquire the behavior associated with this sheet, we need to be attached to the
    // view hierarchy of our parent, otherwise we get an exception that our view is not a child of a
    // CoordinatorLayout. Therefore we do most initialization here instead of in onViewCreated().
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = resolveViewModelDelegate()
        binding.viewModel = viewModel

        behavior = BottomSheetBehavior.from(binding.filterSheet)

        filterAdapter = SelectableFilterChipAdapter(viewModel)
        viewModel.filterChips.observe(
            viewLifecycleOwner,
            Observer {
                filterAdapter.submitFilterList(it)
            }
        )

        binding.recyclerviewFilters.apply {
            adapter = filterAdapter
            setHasFixedSize(true)
            itemAnimator = null
            addOnScrollListener(object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    binding.filtersHeaderShadow.isActivated = recyclerView.canScrollVertically(-1)
                }
            })
            addItemDecoration(
                FlexboxItemDecoration(context).apply {
                    setDrawable(context.getDrawable(R.drawable.divider_empty_margin_small))
                    setOrientation(FlexboxItemDecoration.VERTICAL)
                }
            )
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

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                updateBackPressedCallbackEnabled(newState)
            }
        })

        binding.collapseArrow.setOnClickListener {
            behavior.state = if (behavior.skipCollapsed) STATE_HIDDEN else STATE_COLLAPSED
        }

        binding.filterSheet.doOnLayout {
            val slideOffset = when (behavior.state) {
                STATE_EXPANDED -> 1f
                STATE_COLLAPSED -> 0f
                else /*BottomSheetBehavior.STATE_HIDDEN*/ -> -1f
            }
            updateFilterContentsAlpha(slideOffset)
        }

        if (pendingSheetState != -1) {
            behavior.state = pendingSheetState
            pendingSheetState = -1
        }
        updateBackPressedCallbackEnabled(behavior.state)
    }

    private fun updateFilterContentsAlpha(slideOffset: Float) {
        // Since the content is visible behind the navigation bar, apply a short alpha transition.
        contentAlpha.set(
            slideOffsetToAlpha(slideOffset, ALPHA_CONTENT_START, ALPHA_CONTENT_END)
        )
    }

    private fun updateBackPressedCallbackEnabled(state: Int) {
        backPressedCallback.isEnabled = !(state == STATE_COLLAPSED || state == STATE_HIDDEN)
    }

    fun showFiltersSheet() {
        if (::behavior.isInitialized) {
            behavior.state = STATE_EXPANDED
        } else {
            pendingSheetState = STATE_EXPANDED
        }
    }

    fun hideFiltersSheet() {
        if (::behavior.isInitialized) {
            behavior.state = STATE_HIDDEN
        } else {
            pendingSheetState = STATE_HIDDEN
        }
    }
}
