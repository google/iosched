/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.myio;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;

import com.google.samples.apps.iosched.model.ScheduleItem;
import com.google.samples.apps.iosched.myio.MyIOContract.MyIoPresenter;
import com.google.samples.apps.iosched.myio.MyIOContract.MyIoView;
import com.google.samples.apps.iosched.util.CursorModelLoader;

import java.util.List;

public class MyIOPresenterImpl implements MyIoPresenter {
    private static final int LOADER_SCHEDULE = 1;

    private Context mContext;
    private MyIoView mView;
    private MyIOModel mModel;

    MyIOPresenterImpl(Context context, MyIoView view) {
        mContext = context;
        mView = view;
        mModel = new MyIOModel();
    }

    @Override
    public void initModel(LoaderManager loaderManager) {
        loaderManager.initLoader(LOADER_SCHEDULE, null, mSessionsLoaderCallbacks);
    }

    // -- LoaderCallbacks implementations

    private LoaderCallbacks<List<ScheduleItem>> mSessionsLoaderCallbacks =
            new LoaderCallbacks<List<ScheduleItem>>() {

        @Override
        public Loader<List<ScheduleItem>> onCreateLoader(int id, Bundle args) {
            return new CursorModelLoader<>(mContext, new MyIOScheduleCursorTransform());
        }

        @Override
        public void onLoadFinished(Loader<List<ScheduleItem>> loader, List<ScheduleItem> data) {
            mModel.setScheduleItems(data);
            mView.onScheduleLoaded(mModel);
        }

        @Override
        public void onLoaderReset(Loader<List<ScheduleItem>> loader) {
            mModel.setScheduleItems(null);
            mView.onScheduleLoaded(mModel);
        }
    };
}
