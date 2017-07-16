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

package com.google.samples.apps.iosched.explore;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;

import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.testutils.StubModelHelper;

import java.util.HashMap;

/**
 * A stub {@link ExploreIOModel}, to be injected using {@link com.google.samples.apps.iosched
 * .injection.Injection}. It overrides {@link #requestData(QueryEnum, Model.DataQueryCallback)} to
 * bypass the loader manager mechanism. Use the classes in {@link com.google.samples.apps.iosched
 * .mockdata} to provide the stub cursors.
 */
public class StubExploreIOModel extends ExploreIOModel {

    private HashMap<QueryEnum, Cursor> mFakeData = new HashMap<QueryEnum, Cursor>();

    public StubExploreIOModel(Context context, Cursor sessionsCursor, Cursor tagsCursor) {
        super(context, null, null);
        mFakeData.put(ExploreIOQueryEnum.SESSIONS, sessionsCursor);
        mFakeData.put(ExploreIOQueryEnum.TAGS, tagsCursor);
    }

    /**
     * Overrides the loader manager mechanism by directly calling {@link #onLoadFinished(QueryEnum,
     * Cursor)} with a stub {@link Cursor} as provided in the constructor.
     */
    @Override
    public void requestData(final @NonNull ExploreIOModel.ExploreIOQueryEnum query,
            final @NonNull DataQueryCallback callback) {
        new StubModelHelper<ExploreIOQueryEnum>()
                .overrideLoaderManager(query, callback, mFakeData, mDataQueryCallbacks, this);
    }
}
