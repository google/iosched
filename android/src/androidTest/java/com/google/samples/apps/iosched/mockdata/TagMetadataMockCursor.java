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
