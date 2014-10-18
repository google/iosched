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

import android.app.ActionBar;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.Arrays;
import java.util.Locale;

public class ExpertsDirectoryActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String SCREEN_LABEL = "ExpertsDirectory";

    /**
     * State key for the country currently selected in the spinner
     */
    private static final String STATE_CURRENT_COUNTRY = "current_country";

    /**
     * State key for the city currently selected in the spinner
     */
    private static final String STATE_CURRENT_CITY = "current_city";

    private Spinner mFilterCountriesSpinner;
    private Spinner mFilterCitiesSpinner;

    private String mCurrentCountry;
    private String mCurrentCity;

    private DrawShadowFrameLayout mDrawShadowFrameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_experts_directory);
        AnalyticsManager.sendScreenView(SCREEN_LABEL);

        mDrawShadowFrameLayout = (DrawShadowFrameLayout) findViewById(R.id.main_content);
        mFilterCountriesSpinner = (Spinner) findViewById(R.id.filter_countries);
        mFilterCitiesSpinner = (Spinner) findViewById(R.id.filter_cities);

        // Restore saved states
        if (null != savedInstanceState) {
            mCurrentCountry = savedInstanceState.getString(STATE_CURRENT_COUNTRY);
            mCurrentCity = savedInstanceState.getString(STATE_CURRENT_CITY);
        }

        // Start loading data
        getLoaderManager().restartLoader(CountriesQuery.TOKEN, null, this);

        overridePendingTransition(0, 0);
        registerHideableHeaderView(findViewById(R.id.headerbar));
    }

    private static String getCountryName(String countryCode) {
        return new Locale("", countryCode).getDisplayCountry();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (null != mCurrentCountry) {
            outState.putString(STATE_CURRENT_COUNTRY, mCurrentCountry);
        }
        if (null != mCurrentCity) {
            outState.putString(STATE_CURRENT_CITY, mCurrentCity);
        }
    }

    @Override
    protected void onActionBarAutoShowOrHide(boolean shown) {
        super.onActionBarAutoShowOrHide(shown);
        mDrawShadowFrameLayout.setShadowVisible(shown, shown);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_EXPERTS_DIRECTORY;
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        if (Config.hasExpertsDirectoryExpired()) {
            startActivity(new Intent(this, BrowseSessionsActivity.class));
            finish();
        }

        Fragment frag = getFragmentManager().findFragmentById(R.id.experts_fragment);
        if (frag != null) {
            // configure expert fragment's top clearance to take our overlaid controls (Action Bar
            // and spinner box) into account.
            int actionBarSize = UIUtils.calculateActionBarSize(this);
            int filterBarSize = getResources().getDimensionPixelSize(R.dimen.filterbar_height);
            mDrawShadowFrameLayout.setShadowTopOffset(actionBarSize + filterBarSize);
            ((ExpertsDirectoryFragment) frag).setContentTopClearance(actionBarSize + filterBarSize
                    + getResources().getDimensionPixelSize(R.dimen.explore_grid_padding));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case CountriesQuery.TOKEN: {
                return new CursorLoader(this,
                        ScheduleContract.Experts.CONTENT_URI
                                .buildUpon()
                                .appendQueryParameter(ScheduleContract.QUERY_PARAMETER_DISTINCT, "true")
                                .build(),
                        CountriesQuery.PROJECTION, null, null, ScheduleContract.Experts.EXPERT_COUNTRY
                );
            }
            case CitiesQuery.TOKEN: {
                String selection = null;
                String[] selectionArgs = null;
                if (null != args && null != (mCurrentCountry
                        = args.getString(ScheduleContract.Experts.EXPERT_COUNTRY))) {
                    selection = ScheduleContract.Experts.EXPERT_COUNTRY + " = ?";
                    selectionArgs = new String[]{mCurrentCountry};
                } else {
                    mCurrentCity = null;
                }
                return new CursorLoader(this,
                        ScheduleContract.Experts.CONTENT_URI
                                .buildUpon()
                                .appendQueryParameter(ScheduleContract.QUERY_PARAMETER_DISTINCT, "true")
                                .build(),
                        CitiesQuery.PROJECTION, selection, selectionArgs,
                        ScheduleContract.Experts.EXPERT_CITY
                );
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case CountriesQuery.TOKEN: {
                String[] countries = getStringArray(cursor, CountriesQuery.EXPERT_COUNTRY);
                CountriesAdapter adapter = new CountriesAdapter(countries);
                mFilterCountriesSpinner.setAdapter(adapter);
                if (null != mCurrentCountry) {
                    mFilterCountriesSpinner.setSelection(Arrays.binarySearch(countries, mCurrentCountry) + 1);
                }
                mFilterCountriesSpinner.setOnItemSelectedListener(adapter);
                break;
            }
            case CitiesQuery.TOKEN: {
                String[] cities = getStringArray(cursor, CitiesQuery.EXPERT_CITY);
                CitiesAdapter adapter = new CitiesAdapter(cities);
                mFilterCitiesSpinner.setAdapter(adapter);
                if (null != mCurrentCity) {
                    mFilterCitiesSpinner.setSelection(Arrays.binarySearch(cities, mCurrentCity) + 1);
                }
                // Hide the spinner when there is only one city in this country
                if (1 == cities.length) {
                    mFilterCitiesSpinner.setSelection(1);
                    mFilterCitiesSpinner.setVisibility(View.INVISIBLE);
                } else {
                    mFilterCitiesSpinner.setVisibility(View.VISIBLE);
                }
                mFilterCitiesSpinner.setOnItemSelectedListener(adapter);
                break;
            }
        }
    }

    private String[] getStringArray(Cursor cursor, int columnIndex) {
        String[] countries = new String[cursor.getCount()];
        int i = 0;
        while (cursor.moveToNext()) {
            countries[i++] = cursor.getString(columnIndex);
        }
        return countries;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case CountriesQuery.TOKEN: {
                mFilterCitiesSpinner.setAdapter(null);
                break;
            }
            case CitiesQuery.TOKEN: {
                mFilterCitiesSpinner.setAdapter(null);
                break;
            }
        }
    }

    /**
     * An adapter for countries in the spinner.
     */
    private class CountriesAdapter extends BaseAdapter implements AdapterView.OnItemSelectedListener {
        private final String[] mCountries;

        public CountriesAdapter(String[] countries) {
            mCountries = countries;
        }

        @Override
        public int getCount() {
            return mCountries.length + 1;
        }

        @Override
        public String getItem(int position) {
            if (0 == position) {
                return null;
            }
            return mCountries[position - 1];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getDropDownView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(ExpertsDirectoryActivity.this).inflate(
                        R.layout.explore_spinner_item_dropdown, parent, false);
            }
            bind(view, position);
            return view;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (null == view) {
                view = LayoutInflater.from(ExpertsDirectoryActivity.this).inflate(
                        R.layout.explore_spinner_item, parent, false);
            }
            bind(view, position);
            return view;
        }

        private void bind(View view, int position) {
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            if (0 == position) {
                textView.setText(getString(R.string.experts_directory_all_countries));
            } else {
                textView.setText(getCountryName(getItem(position)));
            }
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mCurrentCountry = getItem(position);
            if (null == mCurrentCountry) { // All countries
                mFilterCitiesSpinner.setVisibility(View.INVISIBLE);
                reloadExpertsList();
            } else {
                Bundle args = new Bundle();
                args.putString(ScheduleContract.Experts.EXPERT_COUNTRY, mCurrentCountry);
                getLoaderManager().restartLoader(CitiesQuery.TOKEN, args,
                        ExpertsDirectoryActivity.this);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            mCurrentCountry = null;
            mCurrentCity = null;
            reloadExpertsList();
        }
    }

    /**
     * An adapter for cities.
     */
    private class CitiesAdapter extends BaseAdapter implements AdapterView.OnItemSelectedListener {
        private final String[] mCities;

        public CitiesAdapter(String[] cities) {
            mCities = cities;
        }

        @Override
        public int getCount() {
            return mCities.length + 1;
        }

        @Override
        public String getItem(int position) {
            if (0 == position) {
                return null;
            }
            return mCities[position - 1];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getDropDownView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = LayoutInflater.from(ExpertsDirectoryActivity.this).inflate(
                        R.layout.explore_spinner_item_dropdown, parent, false);
            }
            bind(view, position);
            return view;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (null == view) {
                view = LayoutInflater.from(ExpertsDirectoryActivity.this).inflate(
                        R.layout.explore_spinner_item, parent, false);
            }
            bind(view, position);
            return view;
        }

        private void bind(View view, int position) {
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            if (0 == position) {
                textView.setText(getString(R.string.experts_directory_all_cities));
            } else {
                textView.setText(getItem(position));
            }
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mCurrentCity = getItem(position);
            reloadExpertsList();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            mCurrentCity = null;
            reloadExpertsList();
        }
    }

    private void reloadExpertsList() {
        ExpertsDirectoryFragment fragment = (ExpertsDirectoryFragment)
                getFragmentManager().findFragmentById(R.id.experts_fragment);
        if (fragment == null) {
            return;
        }

        fragment.reload(mCurrentCountry, mCurrentCity);
    }

    private interface CountriesQuery {
        int TOKEN = 0x1;

        String[] PROJECTION = {
                ScheduleContract.Experts.EXPERT_COUNTRY,
        };

        int EXPERT_COUNTRY = 0;
    }

    private interface CitiesQuery {
        int TOKEN = 0x2;

        String[] PROJECTION = {
                ScheduleContract.Experts.EXPERT_CITY,
        };

        int EXPERT_CITY = 0;
    }

}
