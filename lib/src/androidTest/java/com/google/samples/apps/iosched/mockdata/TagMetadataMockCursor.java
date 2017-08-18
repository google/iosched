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

package com.google.samples.apps.iosched.mockdata;

import android.database.MatrixCursor;

/**
 * This has methods to create stub cursors for tag metadata. To generate different mock cursors,
 * refer to {@link com.google.samples.apps.iosched.debug
 * .OutputMockData#generateMatrixCursorCodeForCurrentRow(Cursor)}.
 */
public class TagMetadataMockCursor {

    public static final String TAG_ID = "1";

    public static final String TAG_ID_NAME = "TYPE_SANDBOXTALKS";

    public static final String TAG_NAME = "Sandbox talks";

    public static final String TAG_CATEGORY = "TYPE";

    public static MatrixCursor getCursorForSingleTagMetadata() {
        String[] data = {TAG_ID, TAG_ID_NAME, TAG_NAME, TAG_CATEGORY, "2", "", "-3355444",""};
        String[] columns = {"_id", "tag_id", "tag_name", "tag_category", "tag_order_in_category",
                "tag_abstract", "tag_color", "tag_photo_url"};
        MatrixCursor matrixCursor = new MatrixCursor(columns);
        matrixCursor.addRow(data);
        return matrixCursor;
    }
}
