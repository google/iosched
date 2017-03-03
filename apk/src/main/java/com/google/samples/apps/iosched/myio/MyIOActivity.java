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

package com.google.samples.apps.iosched.myio;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.PresenterImpl;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.model.ScheduleHelper;
import com.google.samples.apps.iosched.myschedule.MyScheduleModel;
import com.google.samples.apps.iosched.myschedule.MySchedulePagerFragment;
import com.google.samples.apps.iosched.myschedule.ScheduleView;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.SessionsHelper;

public class MyIOActivity extends BaseActivity {

    private static final String SCREEN_LABEL = "My I/O";

    private MyScheduleModel mModel;
    private PresenterImpl<MyScheduleModel, MyScheduleModel.MyScheduleQueryEnum,
            MyScheduleModel.MyScheduleUserActionEnum> mPresenter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.myio_act);

        mModel = ModelProvider.provideMyScheduleModel(
                new ScheduleHelper(this, ScheduleHelper.MODE_STARRED_ITEMS),
                new SessionsHelper(this),
                this);

        final MySchedulePagerFragment contentFragment =
                (MySchedulePagerFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.schedule_content);

        // Each fragment in the pager adapter is an updatable view that the presenter must know
        mPresenter = new PresenterImpl<MyScheduleModel, MyScheduleModel.MyScheduleQueryEnum,
                MyScheduleModel.MyScheduleUserActionEnum>(
                mModel,
                contentFragment.getDayFragments(),
                MyIOModel.MyScheduleUserActionEnum.values(),
                MyIOModel.MyScheduleQueryEnum.values());
    }

    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return NavigationModel.NavigationItemEnum.MY_IO;
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        final Fragment contentFragment = getSupportFragmentManager()
                .findFragmentById(R.id.schedule_content);

        if (contentFragment instanceof ScheduleView) {
            return ((ScheduleView) contentFragment).canSwipeRefreshChildScrollUp();
        }

        return false;
    }

    @Override
    protected String getScreenLabel() {
        return SCREEN_LABEL;
    }
}
