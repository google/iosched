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

package com.google.samples.apps.iosched.map.util;

import android.content.Context;
import android.content.CursorLoader;
import android.net.Uri;

/**
 * Wrapper for CursorLoaders that need to pass room information.
 */
public abstract class SessionLoader extends CursorLoader {

    private String mRoomTitle;
    private String mRoomId;
    private int mRoomType;

    public SessionLoader(Context context, String roomId, String roomTitle, int roomType, Uri uri,
            String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        super(context, uri, projection, selection, selectionArgs, sortOrder);

        mRoomId = roomId;
        mRoomTitle = roomTitle;
        mRoomType = roomType;
    }

    public int getRoomType() {
        return mRoomType;
    }

    public String getRoomId() {
        return mRoomId;
    }

    public String getRoomTitle() {
        return mRoomTitle;
    }


}
