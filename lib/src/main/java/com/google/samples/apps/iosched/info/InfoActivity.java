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

import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.ui.BaseActivity;

public class InfoActivity extends BaseActivity {

    private InfoContract.Presenter mPresenter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_act);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setFullscreenLayout();

        InfoPagerFragment infoPagerFragment = (InfoPagerFragment) getSupportFragmentManager()
                .findFragmentById(R.id.main_content);
        mPresenter = new InfoPresenter(this, infoPagerFragment);
        mPresenter.initEventInfo();
        mPresenter.initAboutInfo();
        mPresenter.initTravelInfo();
        infoPagerFragment.setPresenter(mPresenter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.INFO;
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return true;
    }

    @Override
    protected int getNavigationTitleId() {
        return R.string.title_info;
    }
}
