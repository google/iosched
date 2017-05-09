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
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.model.TagMetadataCursorTransform;
import com.google.samples.apps.iosched.myio.MyIOContract.MyIoPresenter;
import com.google.samples.apps.iosched.myio.MyIOContract.MyIoView;
import com.google.samples.apps.iosched.util.CursorModelLoader;

import java.util.List;

public class MyIOPresenterImpl implements MyIoPresenter {
    private static final int LOADER_SCHEDULE = 1;
    private static final int LOADER_TAG_METADATA = 2;
    private static final int LOADER_BLOCKS = 3;

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
        loaderManager.initLoader(LOADER_TAG_METADATA, null, mTagMetadataLoaderCallbacks);
        loaderManager.initLoader(LOADER_BLOCKS, null, mBlocksLoaderCallbacks);
    }

    @Override
    public void refreshUI(LoaderManager loaderManager) {
        loaderManager.restartLoader(LOADER_SCHEDULE, null, mSessionsLoaderCallbacks);
        loaderManager.restartLoader(LOADER_TAG_METADATA, null, mTagMetadataLoaderCallbacks);
        loaderManager.restartLoader(LOADER_BLOCKS, null, mBlocksLoaderCallbacks);
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
            mModel.setSessionItems(data);
            mView.onScheduleLoaded(mModel);
        }

        @Override
        public void onLoaderReset(Loader<List<ScheduleItem>> loader) {
            mModel.setSessionItems(null);
            mView.onScheduleLoaded(mModel);
        }
    };


    private LoaderCallbacks<List<ScheduleItem>> mBlocksLoaderCallbacks =
            new LoaderCallbacks<List<ScheduleItem>>() {

                @Override
                public Loader<List<ScheduleItem>> onCreateLoader(int id, Bundle args) {
                    return new CursorModelLoader<>(mContext, new MyIOBlocksCursorTransform());
                }

                @Override
                public void onLoadFinished(Loader<List<ScheduleItem>> loader, List<ScheduleItem> data) {
                    mModel.setBlockItems(data);
                    mView.onScheduleLoaded(mModel);
                }

                @Override
                public void onLoaderReset(Loader<List<ScheduleItem>> loader) {
                    mModel.setBlockItems(null);
                    mView.onScheduleLoaded(mModel);
                }
            };



    private LoaderCallbacks<TagMetadata> mTagMetadataLoaderCallbacks =
            new LoaderCallbacks<TagMetadata>() {

        @Override
        public Loader<TagMetadata> onCreateLoader(int id, Bundle args) {
            return new CursorModelLoader<>(mContext, new TagMetadataCursorTransform());
        }

        @Override
        public void onLoadFinished(Loader<TagMetadata> loader, TagMetadata data) {
            mModel.setTagMetadata(data);
            mView.onTagMetadataLoaded(mModel);
        }

        @Override
        public void onLoaderReset(Loader<TagMetadata> loader) {
            mModel.setSessionItems(null);
            mView.onTagMetadataLoaded(mModel);
        }
    };
}
