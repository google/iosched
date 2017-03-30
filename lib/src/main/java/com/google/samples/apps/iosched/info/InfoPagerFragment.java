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
package com.google.samples.apps.iosched.info;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.lib.R;

public class InfoPagerFragment extends Fragment {

    private ViewPager mViewPager;
    private InfoViewPagerAdapter mViewPagerAdapter;
    private TabLayout mTabLayout;
    private int mCurrentPage;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.info_pager_frag, container, false);
        return root;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCurrentPage = 0;
        mViewPager = (ViewPager) view.findViewById(R.id.view_pager);
        mViewPagerAdapter = new InfoViewPagerAdapter(getContext(),
                getChildFragmentManager());
        mViewPager.setAdapter(mViewPagerAdapter);
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
            }
        });

        mViewPager.setPageMargin(getResources()
                .getDimensionPixelSize(R.dimen.my_schedule_page_margin));
        mViewPager.setPageMarginDrawable(R.drawable.page_margin);

        setCurrentPage();
    }

    private void setCurrentPage() {
        if (mViewPager != null) {
            mViewPager.setCurrentItem(mCurrentPage);
        }
    }
}
