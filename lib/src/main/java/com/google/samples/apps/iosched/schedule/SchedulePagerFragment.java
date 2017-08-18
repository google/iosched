/*
 * Copyright (c) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.schedule;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.CoordinatorLayout.Behavior;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.TimeUtils;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class SchedulePagerFragment extends Fragment implements ScheduleView {

    /**
     * The key used to save the tags for {@link ScheduleSingleDayFragment}s so the automatically
     * recreated fragments can be reused by {@link #mViewPagerAdapter}.
     */
    private static final String SINGLE_DAY_FRAGMENTS_TAGS = "single_day_fragments_tags";

    /**
     * The key used to save the position in the {@link #mViewPagerAdapter} for the current {@link
     * ScheduleSingleDayFragment}s.
     */
    private static final String CURRENT_SINGLE_DAY_FRAGMENT_POSITION =
            "current_single_day_fragments_position";

    /**
     * This is used for narrow mode only, to switch between days, it is null in wide mode
     */
    private ViewPager mViewPager;

    /**
     * This is used for narrow mode only, it is null in wide mode. Each page in the {@link
     * #mViewPager} is a {@link ScheduleSingleDayFragment}.
     */
    private ScheduleDayViewPagerAdapter mViewPagerAdapter;

    /**
     * This is used for narrow mode only, to display the conference days, it is null in wide mode
     */
    private TabLayout mTabLayout;

    /**
     * Bar that appears when there are active filters
     */
    private AppBarLayout mAppBar;
    private View mContent;
    private View mFiltersBar;
    private TextView mFiltersDescription;
    private View mClearFilters;
    private final Rect mFiltersBarClip = new Rect();

    /**
     * During the conference, this is set to the current day, eg 1 for the first day, 2 for the
     * second etc Outside of conference period, this is set to 1.
     */
    private int mToday;

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        ScheduleSingleDayFragment currentFragment =
                mViewPagerAdapter.getFragments()[mViewPager.getCurrentItem()];
        if (currentFragment.getUserVisibleHint()) {
            return ViewCompat.canScrollVertically(currentFragment.getRecyclerView(), -1);
        }
        return false;
    }

    @Override
    public void onFiltersChanged(TagFilterHolder filters) {
        CharSequence filtersDesc = filters.describeFilters(getResources(), getContext().getTheme());
        mFiltersDescription.setText(filtersDesc);
        if (filters.hasAnyFilters()) {
            mFiltersBar.setVisibility(VISIBLE);
            mContent.setTranslationY(mFiltersBar.getHeight());
            offsetFilters(mFiltersBar.getHeight(), 0);
        } else {
            mFiltersBar.setVisibility(INVISIBLE);
            offsetFilters(mFiltersBar.getHeight(), 0);
            mContent.setTranslationY(0f);
        }
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable
    final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.schedule_pager_fragment, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable
    final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String[] singleDayFragmentsTags = null;
        int currentSingleDayFragment = 0;
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SINGLE_DAY_FRAGMENTS_TAGS)) {
                singleDayFragmentsTags = savedInstanceState.getStringArray(
                        SINGLE_DAY_FRAGMENTS_TAGS);
            }
            if (savedInstanceState.containsKey(CURRENT_SINGLE_DAY_FRAGMENT_POSITION)) {
                currentSingleDayFragment = savedInstanceState.getInt(
                        CURRENT_SINGLE_DAY_FRAGMENT_POSITION);
            }
        }

        mContent = view.findViewById(R.id.swipe_refresh_layout);
        mViewPager = (ViewPager) view.findViewById(R.id.view_pager);
        mViewPagerAdapter = new ScheduleDayViewPagerAdapter(getContext(),
                getChildFragmentManager(), ScheduleModel.showPreConferenceData(getContext()));
        mViewPagerAdapter.setRetainedFragmentsTags(singleDayFragmentsTags);
        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setCurrentItem(currentSingleDayFragment);

        mTabLayout = (TabLayout) view.findViewById(R.id.sliding_tabs);
        mTabLayout.setupWithViewPager(mViewPager);

        // Add a listener for any reselection events
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(final TabLayout.Tab tab) {
            }

            @Override
            public void onTabUnselected(final TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(final TabLayout.Tab tab) {
                mViewPagerAdapter.getFragments()[tab.getPosition()].resetListPosition();
            }
        });

        mViewPager.setPageMargin(getResources()
                .getDimensionPixelSize(R.dimen.my_schedule_page_margin));
        mViewPager.setPageMarginDrawable(R.drawable.page_margin);
        mViewPager.setOffscreenPageLimit(3);

        mAppBar = (AppBarLayout) view.findViewById(R.id.appbar);
        mFiltersBar = view.findViewById(R.id.filters_bar);
        mFiltersDescription = (TextView) mFiltersBar.findViewById(R.id.filters_description);
        mFiltersDescription.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();
                if (activity instanceof ScheduleViewParent) {
                    ((ScheduleViewParent) activity).openFilterDrawer();
                }
            }
        });
        mClearFilters = mFiltersBar.findViewById(R.id.clear_filters);
        mClearFilters.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity activity = getActivity();
                if (activity instanceof ScheduleViewParent) {
                    ((ScheduleViewParent) activity).onRequestClearFilters();
                }
            }
        });

        calculateCurrentDay();
        if (mViewPager != null) {
            scrollToConferenceDay(mToday);
        }
        View header = view.findViewById(R.id.header_anim);
        if (header instanceof ImageView) {
            AnimatedVectorDrawable avd = (AnimatedVectorDrawable) ContextCompat.getDrawable(
                    getContext(), R.drawable.avd_header_schedule);
            ((ImageView) header).setImageDrawable(avd);
            avd.start();
        }

        /* Our desired UX is to have a collapsing toolbar (to house the animated headers) and to use
           the quick-return pattern on the filters bar. This isn't supported by stock widgets so we
           extent ScrollingViewBehaviour and use nested scroll events to achieve this */
        final AppBarCollapsedListener appBarCollapsedListener = new AppBarCollapsedListener();
        mAppBar.addOnOffsetChangedListener(appBarCollapsedListener);
        ((CoordinatorLayout.LayoutParams) mContent.getLayoutParams())
                .setBehavior(new AppBarLayout.ScrollingViewBehavior() {

            @Override
            public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child,
                       View directTargetChild, View target, int nestedScrollAxes) {
                return (nestedScrollAxes & View.SCROLL_AXIS_VERTICAL) != 0
                        && mFiltersBar.getVisibility() == VISIBLE;
            }

            @Override
            public void onNestedScroll(CoordinatorLayout coordinatorLayout, View child,
                  View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
                float filtersOffset = mFiltersBar.getTranslationY();
                int filtersHeight = mFiltersBar.getHeight();
                if (dyConsumed > 0
                        && appBarCollapsedListener.isCollapsed()
                        && filtersOffset > -filtersHeight) {
                    int offset = (int) Math.max(filtersOffset - dyConsumed, -filtersHeight);
                    offsetFilters(filtersHeight, offset);
                } else if (dyConsumed < 0 && filtersOffset < 0f) {
                    int offset = (int) Math.min(filtersOffset - dyConsumed, 0f);
                    offsetFilters(filtersHeight, offset);
                }
            }});

        AnalyticsHelper.sendScreenView("Schedule for Day " + mToday, getActivity());
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mViewPagerAdapter != null && mViewPagerAdapter.getFragments() != null) {
            ScheduleSingleDayFragment[] singleDayFragments = mViewPagerAdapter.getFragments();
            String[] tags = new String[singleDayFragments.length];
            for (int i = 0; i < tags.length; i++) {
                tags[i] = singleDayFragments[i].getTag();
            }
            outState.putStringArray(SINGLE_DAY_FRAGMENTS_TAGS, tags);
            outState.putInt(CURRENT_SINGLE_DAY_FRAGMENT_POSITION, mViewPager.getCurrentItem());
        }
    }

    private void offsetFilters(int filtersHeight, int offset) {
        mFiltersBar.setTranslationY(offset);
        mFiltersBarClip.set(0, -offset, mFiltersBar.getWidth(), filtersHeight);
        mFiltersBar.setClipBounds(mFiltersBarClip);
        mContent.setTranslationY(filtersHeight + offset);
    }

    private void calculateCurrentDay() {
        final long now = TimeUtils.getCurrentTime(getContext());

        // If we are before or after the conference, the first day is considered the current day
        mToday = 0;

        for (int i = 0; i < Config.CONFERENCE_DAYS.length; i++) {
            if (now >= Config.CONFERENCE_DAYS[i][0] && now <= Config.CONFERENCE_DAYS[i][1]) {
                // mToday is set to 1 for the first day, 2 for the second etc
                mToday = i;
                break;
            }
        }
    }

    /**
     * @param day The zero-indexed conference day.
     */
    public void scrollToConferenceDay(int day) {
        int preConferenceDays = ScheduleModel.showPreConferenceData(getContext()) ? 1 : 0;
        mViewPager.setCurrentItem(day + preConferenceDays);
    }

    public ScheduleSingleDayFragment[] getDayFragments() {
        return mViewPagerAdapter != null ? mViewPagerAdapter.getFragments() : null;
    }

    /**
     * A listener which tracks whether the {@link AppBarLayout} is collapsed.
     */
    static class AppBarCollapsedListener implements AppBarLayout.OnOffsetChangedListener {

        private boolean isCollapsed = false;

        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            isCollapsed = verticalOffset == -appBarLayout.getTotalScrollRange();
        }

        boolean isCollapsed() {
            return isCollapsed;
        }
    }

    /**
     * A {@link Behavior} that works with {@code layout_anchor} to position the filters bar
     * <strong>below</strong> the {@link AppBarLayout} throughout it's collapse.
     */
    @Keep // instantiated from layout/schedule_pager_fragment
    public static class FilterPositionBehavior extends Behavior<LinearLayout> {

        @Keep public FilterPositionBehavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, LinearLayout child,
                                       View dependency) {
            return dependency instanceof AppBarLayout;
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, LinearLayout child,
                                              View dependency) {
            if (dependency.getBottom() == child.getTop()) return false;
            child.offsetTopAndBottom(dependency.getBottom() - child.getTop());
            return true;
        }
    }

}
