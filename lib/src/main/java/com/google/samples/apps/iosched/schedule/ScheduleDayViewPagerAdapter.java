/*
 * Copyright 2016 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.schedule;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.util.TimeUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * The {@link ScheduleActivity} uses a {@link android.support.v4.view.ViewPager} in narrow mode,
 * where each page shows the schedule for the day, using a {@link ScheduleSingleDayFragment}.
 */
public class ScheduleDayViewPagerAdapter extends FragmentPagerAdapter {

    private final static String TAG = makeLogTag(ScheduleDayViewPagerAdapter.class);

    private Context mContext;

    private boolean mShowPreConferenceDay;

    private ScheduleSingleDayFragment[] mFragments;

    private FragmentManager mFragmentManager;

    public ScheduleDayViewPagerAdapter(Context context, FragmentManager fm,
            boolean showPreConferenceDay) {
        super(fm);
        mShowPreConferenceDay = showPreConferenceDay;
        mContext = context;
        mFragmentManager = fm;
    }

    @Override
    public ScheduleSingleDayFragment getItem(int position) {
        LOGD(TAG, "Creating fragment #" + position);

        // Reuse cached fragment if present
        if (mFragments != null && mFragments.length > position && mFragments[position] != null) {
            return mFragments[position];
        }

        ScheduleSingleDayFragment frag = new ScheduleSingleDayFragment();
        Bundle args = new Bundle();

        // 1 for the first day of the conference, 2 for the second etc and 0 for the pre conference
        // day if any
        args.putInt(ScheduleActivity.ARG_CONFERENCE_DAY_INDEX,
                mShowPreConferenceDay ? position : position + 1);

        frag.setArguments(args);

        if (mFragments == null) {
            mFragments = new ScheduleSingleDayFragment[getCount()];
        }
        mFragments[position] = frag;
        return frag;
    }

    @Override
    public int getCount() {
        return Config.CONFERENCE_DAYS.length + (mShowPreConferenceDay ? 1 : 0);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return TimeUtils.getDayName(mContext, position - (mShowPreConferenceDay ? 1 : 0));
    }

    /**
     * @return all the cached {@link ScheduleSingleDayFragment}s used by this Adapter.
     */
    public ScheduleSingleDayFragment[] getFragments() {
        if (mFragments == null) {
            // Force creating the fragments
            int count = getCount();
            for (int i = 0; i < count; i++) {
                getItem(i);
            }
        }
        return mFragments;
    }

    /**
     * When the device changes orientation, the {@link ScheduleSingleDayFragment}s are recreated
     * by the system, and they have the same tag ids as the ones previously used. Therefore, this
     * sets the cached fragments to the ones recreated by the system. This must be called before any
     * call to {@link #getItem(int)} or {@link #getFragments()} (note that when fragments are
     * recreated after orientation change, the {@link FragmentPagerAdapter} doesn't call {@link
     * #getItem(int)}.)
     *
     * @param tags the tags of the retained {@link ScheduleSingleDayFragment}s. Ignored if null
     *             or empty.
     */
    public void setRetainedFragmentsTags(String[] tags) {
        if (tags != null && tags.length > 0) {
            mFragments = new ScheduleSingleDayFragment[tags.length];
            for (int i = 0; i < tags.length; i++) {
                ScheduleSingleDayFragment fragment =
                        (ScheduleSingleDayFragment) mFragmentManager.findFragmentByTag(tags[i]);
                mFragments[i] = fragment;
                if (fragment == null) {
                    LOGE(TAG, "Fragment with existing tag " + tags[i] + " not found!");
                    // No retained fragment (this happens if the fragment hadn't been shown before,
                    // because the tag on it would have been null in that case), so instantiate it
                    getItem(i);
                }
            }
        }
    }
}


