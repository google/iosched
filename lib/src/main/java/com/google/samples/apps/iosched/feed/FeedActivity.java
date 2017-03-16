package com.google.samples.apps.iosched.feed;

/*
 * Copyright (c) 2016 Google Inc.
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

import android.os.Bundle;

import com.google.samples.apps.iosched.archframework.PresenterImpl;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.ui.BaseActivity;

/**
 * This shows a list of cards that present key updates on the conference. The cards reside in a
 * {@link RecyclerView} and the content in each card comes from a Firebase real time database.
 * This shows the schedule of the logged in user, organised per day.
 */

public class FeedActivity extends BaseActivity {

    private static final String SCREEN_LABEL = "Feed";

    FeedModel mModel;
    private PresenterImpl<FeedModel, FeedModel.FeedQueryEnum,
            FeedModel.FeedUserActionEnum> mPresenter;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feed_act);



        mModel = ModelProvider.provideFeedModel(this);

        final FeedFragment contentFragment = FeedFragment.getInstance(getSupportFragmentManager());

        // Each fragment in the pager adapter is an updatable view that the presenter must know
        mPresenter = new PresenterImpl<>(
                mModel,
                contentFragment,
                FeedModel.FeedUserActionEnum.values(),
                FeedModel.FeedQueryEnum.values());
    }

    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.FEED;
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return true;
    }

    @Override
    protected String getScreenLabel() {
        return SCREEN_LABEL;
    }
}
