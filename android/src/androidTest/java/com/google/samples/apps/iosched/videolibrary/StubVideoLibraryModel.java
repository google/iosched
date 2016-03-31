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

package com.google.samples.apps.iosched.videolibrary;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.archframework.QueryEnumHelper;
import com.google.samples.apps.iosched.testutils.StubModelHelper;

import java.util.HashMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A stub {@link VideoLibraryModel}, to be injected using {@link com.google.samples.apps.iosched
 * .injection.Injection}. It overrides {@link #requestData(QueryEnum, DataQueryCallback)} to bypass
 * the loader manager mechanism. Use the classes in {@link com.google.samples.apps.iosched.mockdata}
 * to provide the stub cursors.
 */
public class StubVideoLibraryModel extends VideoLibraryModel {

    private HashMap<QueryEnum, Cursor> mFakeData = new HashMap<QueryEnum, Cursor>();

    public StubVideoLibraryModel(Context context, Cursor videosCursor, Cursor filterCursor) {
        super(context, null, null, null, null);
        mFakeData.put(VideoLibraryQueryEnum.VIDEOS, videosCursor);
        mFakeData.put(VideoLibraryQueryEnum.FILTERS, filterCursor);
    }

    /**
     * Overrides the loader manager mechanism by directly calling {@link #onLoadFinished(QueryEnum,
     * Cursor)} with a stub {@link Cursor} as provided in the constructor.
     */
    @Override
    public void requestData(final @NonNull VideoLibraryQueryEnum query,
            final @NonNull DataQueryCallback callback) {
        new StubModelHelper<VideoLibraryModel.VideoLibraryQueryEnum>()
                .overrideLoaderManager(query, callback, mFakeData, mDataQueryCallbacks, this);
    }

    /**
     * Overrides the loader manager mechanism used when changing filter, by directly calling {@link
     * #onLoadFinished(QueryEnum, Cursor)} with a stub {@link Cursor} as provided in the
     * constructor.
     */
    @Override
    public void deliverUserAction(@NonNull VideoLibraryUserActionEnum action, @Nullable Bundle args,
            @NonNull UserActionCallback callback) {
        checkNotNull(callback);
        checkNotNull(action);
        switch (action) {
            case CHANGE_FILTER:
                mDataUpdateCallbacks.put((Integer) args.get(KEY_RUN_QUERY_ID), callback);
                mUserActionsLaunchingQueries.put((Integer) args.get(KEY_RUN_QUERY_ID), action);
                onLoadFinished((VideoLibraryQueryEnum) QueryEnumHelper
                                .getQueryForId((Integer) args.get(KEY_RUN_QUERY_ID),
                                        VideoLibraryQueryEnum.values()),
                        mFakeData.get(VideoLibraryQueryEnum.VIDEOS));
                break;
            default:
                processUserAction(action, args, callback);
        }
    }
}
