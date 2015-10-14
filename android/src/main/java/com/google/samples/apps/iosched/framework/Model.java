/*
 * Copyright 2015 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.framework;

import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * A Model is a class used to manipulate stored data, as well as provide getters for the data. It
 * provides the {@link Presenter} with an interface through which to load and update the data (MVP
 * architectural pattern).
 * <p />
 * Typically, a Model loads its initial data using at least one {@link QueryEnum}. The Model
 * obtains the data from the {@link com.google.samples.apps.iosched.provider.ScheduleProvider} by
 * creating a {@link android.content.CursorLoader} and then parsing the received
 * {@link android.database.Cursor}.
 * <p />
 * Additionally, when a {@link UserActionEnum} is received, the model updates both its own data and
 * the stored data by making an update or insert call on the
 * {@link com.google.samples.apps.iosched.provider.ScheduleProvider}.
 */
public interface Model {

    /**
     * @return an array of {@link QueryEnum} that can be processed by the model
     */
    public QueryEnum[] getQueries();

    /**
     * Updates the data saved in the model from the {@code cursor} and associated {@code query}.
     *
     * @return true if the data could be read properly from cursor.
     */
    public boolean readDataFromCursor(Cursor cursor, QueryEnum query);

    /**
     * Creates the cursor loader for the given loader id and data source {@code uri}.
     * <p/>
     * The {@code loaderId} corresponds to the id of the query, as defined in {@link QueryEnum}. The
     * {@code args} may contain extra arguments required to create the query.
     * <p/>
     * The returned cursor loader is managed by the {@link android.app.LoaderManager}, as part
     * of the {@link android.app.Fragment}
     *
     * @return the cursor loader.
     */
    public Loader<Cursor> createCursorLoader(int loaderId, Uri uri, Bundle args);

    /**
     * Updates this Model according to a user {@code action} and {@code args}.
     * <p />
     * Add the constants used to store values in the bundle to the Model implementation class as
     * final static protected strings.
     *
     * @return true if successful.
     */
    public boolean requestModelUpdate(UserActionEnum action, @Nullable Bundle args);
}
