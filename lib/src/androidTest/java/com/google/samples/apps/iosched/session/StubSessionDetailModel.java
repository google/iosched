/*
 * Copyright 2017 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.session;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.testutils.StubModelHelper;

import java.util.HashMap;

/**
 * A stub {@link SessionDetailModel}, to be injected using {@link com.google.samples.apps.iosched
 * .injection.ModelProvider}. It overrides {@link #requestData(QueryEnum, DataQueryCallback)} to
 * bypass the loader manager mechanism. Use the classes in {@link com.google.samples.apps.iosched
 * .mockdata}
 * to provide the stub cursors.
 */
public class StubSessionDetailModel extends SessionDetailModel {

    private HashMap<SessionDetailQueryEnum, Cursor> mFakeData = new HashMap<>();

    public StubSessionDetailModel(Uri sessionUri, Context context, Cursor sessionCursor,
            Cursor speakersCursor, Cursor tagMetadataCursor) {
        super(sessionUri, context, null, null);
        mFakeData.put(SessionDetailQueryEnum.SESSIONS, sessionCursor);
        mFakeData.put(SessionDetailQueryEnum.SPEAKERS, speakersCursor);
        mFakeData.put(SessionDetailQueryEnum.TAG_METADATA, tagMetadataCursor);
    }

    /**
     * Overrides the loader manager mechanism by directly calling {@link #onLoadFinished(QueryEnum,
     * Cursor)} with a stub {@link Cursor} as provided in the constructor.
     */
    @Override
    public void requestData(final @NonNull SessionDetailQueryEnum query,
            final @NonNull DataQueryCallback<SessionDetailQueryEnum> callback) {
        new StubModelHelper<SessionDetailModel.SessionDetailQueryEnum>()
                .overrideLoaderManager(query, callback, mFakeData, mDataQueryCallbacks, this);
    }
}
