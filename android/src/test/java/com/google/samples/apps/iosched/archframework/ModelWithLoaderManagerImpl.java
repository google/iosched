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

package com.google.samples.apps.iosched.archframework;

import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * A stub implementation of abstract class {@link ModelWithLoaderManager}, used for testing concrete
 * methods only of the abstract class.
 */
public class ModelWithLoaderManagerImpl extends ModelWithLoaderManager {

    public static boolean READ_SUCCESS = false;

    public ModelWithLoaderManagerImpl(QueryEnum[] queries, UserActionEnum[] userActions,
            LoaderManager loaderManager) {
        super(queries, userActions, loaderManager);
    }

    @Override
    public void processUserAction(UserActionEnum action, @Nullable Bundle args,
            UserActionCallback callback) {

    }

    @Override
    public Loader<Cursor> createCursorLoader(QueryEnum query, Bundle args) {
        return null;
    }

    @Override
    public boolean readDataFromCursor(Cursor cursor, QueryEnum query) {
        return READ_SUCCESS;
    }

    @Override
    public void cleanUp() {
        // Do nothing
    }
}
