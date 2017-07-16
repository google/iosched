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

package com.google.samples.apps.iosched.explore;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.ui.SearchActivity;
import com.google.samples.apps.iosched.util.AnalyticsHelper;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Display a summary of what is happening at Google I/O this year. Theme and topic cards are
 * displayed based on the session data. Conference messages are also displayed as cards..
 */
public class ExploreIOActivity extends BaseActivity implements Toolbar.OnMenuItemClickListener {

    private static final String TAG = makeLogTag(ExploreIOActivity.class);

    private static final String SCREEN_LABEL = "Explore I/O";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent launchIntent = getIntent();
        if (launchIntent != null && (!Intent.ACTION_MAIN.equals(launchIntent.getAction())
                || !launchIntent.hasCategory(Intent.CATEGORY_LAUNCHER))) {
            overridePendingTransition(0, 0);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.explore_io_act);
        setTitle(R.string.title_explore);

        // ANALYTICS SCREEN: View the Explore I/O screen
        // Contains: Nothing (Page name is a constant)
        AnalyticsHelper.sendScreenView(SCREEN_LABEL);
    }

    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.EXPLORE;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Add the search button to the toolbar.
        Toolbar toolbar = getToolbar();
        toolbar.inflateMenu(R.menu.explore_io_menu);
        toolbar.setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_search:
                startActivity(new Intent(this, SearchActivity.class));
                return true;
        }
        return false;
    }
}
