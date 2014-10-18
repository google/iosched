/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class VideoLibraryActivity extends BaseActivity implements VideoLibraryFragment.Callbacks {
    private static final String TAG = makeLogTag(VideoLibraryActivity.class);
    private static final String SCREEN_LABEL = "Video Library";

    private static final String STATE_KEY_YEAR = "com.google.samples.apps.iosched.STATE_KEY_YEAR";
    private static final String STATE_KEY_TOPIC = "com.google.samples.apps.iosched.STATE_KEY_TOPIC";

    private ArrayList<Integer> mYears = new ArrayList<Integer>();
    private ArrayList<String> mTopics = new ArrayList<String>();

    private DrawShadowFrameLayout mDrawShadowFrameLayout;

    private int mSelectedYear = 0;
    private String mSelectedTopic = "";

    private int mYearToRestore = 0;
    private String mTopicToRestore = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_video_library);

        mDrawShadowFrameLayout = (DrawShadowFrameLayout) findViewById(R.id.main_content);

        /* [ANALYTICS:SCREEN]
         * TRIGGER:   View the Video Library screen.
         * LABEL:     'Video Library'
         * [/ANALYTICS]
         */
        AnalyticsManager.sendScreenView(SCREEN_LABEL);
        LOGD("Tracker", SCREEN_LABEL);

        overridePendingTransition(0, 0);
        registerHideableHeaderView(findViewById(R.id.headerbar));

        if (savedInstanceState != null) {
            mYearToRestore = savedInstanceState.getInt(STATE_KEY_YEAR, 0);
            mTopicToRestore = savedInstanceState.getString(STATE_KEY_TOPIC, "");
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        enableActionBarAutoHide((CollectionView) findViewById(R.id.videos_collection_view));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_KEY_YEAR, mSelectedYear);
        outState.putString(STATE_KEY_TOPIC, mSelectedTopic);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_VIDEO_LIBRARY;
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();

        Fragment frag = getFragmentManager().findFragmentById(R.id.videos_fragment);
        if (frag != null) {
            // configure video fragment's top clearance to take our overlaid controls (Action Bar
            // and spinner box) into account.
            int actionBarSize = UIUtils.calculateActionBarSize(this);
            int filterBarSize = getResources().getDimensionPixelSize(R.dimen.filterbar_height);
            mDrawShadowFrameLayout.setShadowTopOffset(actionBarSize + filterBarSize);
            ((VideoLibraryFragment) frag).setContentTopClearance(actionBarSize + filterBarSize
                    + getResources().getDimensionPixelSize(R.dimen.explore_grid_padding));
        }
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        mDrawShadowFrameLayout.setShadowVisible(shown, shown);
    }

    private void onYearSelected(int year) {
        if (mSelectedYear == year) {
            return;
        }
        LOGD(TAG, "Year selected: " + year);
        VideoLibraryFragment frag = (VideoLibraryFragment) getFragmentManager()
                .findFragmentById(R.id.videos_fragment);
        if (frag == null) {
            LOGE(TAG, "Videos fragment not found.");
            return;
        }
        mSelectedYear = year;

        /* [ANALYTICS:EVENT]
         * TRIGGER:   Select a year on the Years dropdown in the Video Library.
         * CATEGORY:  'Video Library'
         * ACTION:    selectyear
         * LABEL:     year (e.g. 2014, 2013, 2012)
         * [/ANALYTICS]
         */
        AnalyticsManager.sendEvent("Video Library", "selectyear", String.valueOf(year), 0L);

        frag.setFilterAndReload(mSelectedYear, mSelectedTopic);
    }

    private void onTopicSelected(String topic) {
        if (mSelectedTopic.equals(topic)) {
            return;
        }
        LOGD(TAG, "Topic selected: " + topic);
        VideoLibraryFragment frag = (VideoLibraryFragment) getFragmentManager()
                .findFragmentById(R.id.videos_fragment);
        if (frag == null) {
            LOGE(TAG, "Videos fragment not found.");
            return;
        }
        mSelectedTopic = topic;

        /* [ANALYTICS:EVENT]
         * TRIGGER:   Select a topic on the topics dropdown in the Video Library.
         * CATEGORY:  'Video Library'
         * ACTION:    selecttopic
         * LABEL:     year (e.g. "Android", "Chrome", ...)
         * [/ANALYTICS]
         */
        AnalyticsManager.sendEvent("Video Library", "selecttopic", topic, 0L);


        frag.setFilterAndReload(mSelectedYear, mSelectedTopic);
    }

    @Override
    public void onAvailableVideoYearsChanged(ArrayList<Integer> years) {
        LOGD(TAG, "Got list of available video years, " + years.size() + " items.");
        Spinner yearsSpinner = (Spinner) findViewById(R.id.video_filter_spinner_year);
        mYears.clear();

        if (yearsSpinner != null) {
            ArrayList<String> items = new ArrayList<String>();
            int positionToSelect = -1;

            mYears.add(0); // "All years"
            items.add(getString(R.string.all_years));

            // sort years in decreasing order (start with most recent)
            Collections.sort(years, new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return b.compareTo(a);
                }
            });

            for (int year : years) {
                items.add(getString(R.string.google_i_o_year, year));
                mYears.add(year);
                if (mYearToRestore > 0 && year == mYearToRestore) {
                    positionToSelect = items.size() - 1;
                }
            }
            populateSpinner(yearsSpinner, items);
            yearsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view,
                        int position, long id) {
                    if (position >= 0 && position < mYears.size()) {
                        onYearSelected(mYears.get(position));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });

            if (positionToSelect > 0) {
                yearsSpinner.setSelection(positionToSelect);
                mYearToRestore = 0;
            }
        } else {
            // should not happen...
            LOGE(TAG, "Years spinner not found (Activity not initialized yet?).");
        }
    }

    @Override
    public void onAvailableVideoTopicsChanged(ArrayList<String> availableTopics) {
        LOGD(TAG, "Got list of available video topics, " + availableTopics.size() + " items.");

        // make a sorted list of topics
        ArrayList<String> sortedTopics = new ArrayList<String>();
        sortedTopics.addAll(availableTopics);
        Collections.sort(sortedTopics);

        Spinner topicsSpinner = (Spinner) findViewById(R.id.video_filter_spinner_topic);
        mTopics.clear();
        if (topicsSpinner != null) {
            ArrayList<String> items = new ArrayList<String>();
            int positionToSelect = -1;
            items.add(getString(R.string.all_topics));
            mTopics.add(""); // represents "All topics"
            for (String topic : sortedTopics) {
                items.add(topic);
                mTopics.add(topic);
                if (!TextUtils.isEmpty(mTopicToRestore) && mTopicToRestore.equals(topic)) {
                    positionToSelect = items.size() - 1;
                }
            }
            populateSpinner(topicsSpinner, items);
            topicsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                    if (position >= 0 && position < mTopics.size()) {
                        onTopicSelected(mTopics.get(position));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {}
            });

            if (positionToSelect > 0) {
                topicsSpinner.setSelection(positionToSelect);
                mTopicToRestore = "";
            }
        } else {
            // should not happen...
            LOGE(TAG, "Topics spinner not found (Activity not initialized yet?).");
        }
    }

    private void populateSpinner(Spinner spinner, ArrayList<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.explore_spinner_item,
                android.R.id.text1, items);
        adapter.setDropDownViewResource(R.layout.video_library_spinner_item_dropdown);
        spinner.setAdapter(adapter);
    }
}
