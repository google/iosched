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

import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.google.samples.apps.iosched.debug.DebugFragment;
import com.google.samples.apps.iosched.info.about.AboutInfo;
import com.google.samples.apps.iosched.info.event.EventFragment;
import com.google.samples.apps.iosched.info.event.EventInfo;
import com.google.samples.apps.iosched.info.about.AboutFragment;
import com.google.samples.apps.iosched.info.settings.SettingsFragment;
import com.google.samples.apps.iosched.info.travel.TravelFragment;
import com.google.samples.apps.iosched.info.travel.TravelInfo;
import com.google.samples.apps.iosched.lib.BuildConfig;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class InfoViewPagerAdapter extends FragmentPagerAdapter {

    private final static String TAG = makeLogTag(InfoViewPagerAdapter.class);

    private final static int NUM_PAGES = BuildConfig.DEBUG ? 5 : 4;
    private final static int EVENT_INDEX = 0;
    private final static int TRAVEL_INDEX = 1;
    private final static int SETTINGS_INDEX = 2;
    private final static int ABOUT_INDEX = 3;
    private final static int DEBUG_INDEX = 4;

    private Context mContext;

    private BaseInfoFragment[] mFragments;

    private FragmentManager mFragmentManager;

    public InfoViewPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
        mFragmentManager = fm;
    }

    @Override
    public BaseInfoFragment getItem(int position) {
        LOGD(TAG, "Creating BaseInfoFragment #" + position);

        // Reuse cached fragment if present
        if (mFragments != null && mFragments.length > position && mFragments[position] != null) {
            return mFragments[position];
        }

        if (mFragments == null) {
            mFragments = new BaseInfoFragment[getCount()];
        }

        switch (position) {
            case EVENT_INDEX:
                mFragments[position] = new EventFragment();
                break;
            case TRAVEL_INDEX:
                mFragments[position] = new TravelFragment();
                break;
            case ABOUT_INDEX:
                mFragments[position] = new AboutFragment();
                break;
            case SETTINGS_INDEX:
                mFragments[position] = new SettingsFragment();
                break;
            case DEBUG_INDEX:
                mFragments[position] = new DebugFragment();
                break;
        }
        return mFragments[position];
    }

    public void updateEventInfo(EventInfo eventInfo) {
        BaseInfoFragment infoFragment = getItem(EVENT_INDEX);
        if (infoFragment instanceof EventFragment) {
            ((EventFragment) infoFragment).updateInfo(eventInfo);
        }
    }

    public void updateTravelInfo(TravelInfo travelInfo) {
        BaseInfoFragment infoFragment = getItem(TRAVEL_INDEX);
        if (infoFragment instanceof TravelFragment) {
            ((TravelFragment) infoFragment).updateInfo(travelInfo);
        }
    }

    public void updateAboutInfo(AboutInfo aboutInfo) {
        BaseInfoFragment infoFragment = getItem(ABOUT_INDEX);
        if (infoFragment instanceof AboutFragment) {
            ((AboutFragment) infoFragment).updateInfo(aboutInfo);
        }
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return getItem(position).getTitle(mContext.getResources());
    }

    /**
     * @return all the cached {@link BaseInfoFragment}s used by this Adapter.
     */
    public BaseInfoFragment[] getFragments() {
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
     * When the device changes orientation, the {@link BaseInfoFragment}s are recreated
     * by the system, and they have the same tag ids as the ones previously used. Therefore, this
     * sets the cached fragments to the ones recreated by the system. This must be called before any
     * call to {@link #getItem(int)} or {@link #getFragments()} (note that when fragments are
     * recreated after orientation change, the {@link FragmentPagerAdapter} doesn't call {@link
     * #getItem(int)}.)
     *
     * @param tags the tags of the retained {@link BaseInfoFragment}s. Ignored if null
     *             or empty.
     */
    public void setRetainedFragmentsTags(String[] tags) {
        if (tags != null && tags.length > 0) {
            mFragments = new BaseInfoFragment[tags.length];
            for (int i = 0; i < tags.length; i++) {
                BaseInfoFragment fragment =
                        (BaseInfoFragment) mFragmentManager.findFragmentByTag(tags[i]);
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
