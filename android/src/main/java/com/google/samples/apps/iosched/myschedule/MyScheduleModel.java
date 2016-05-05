package com.google.samples.apps.iosched.myschedule;

import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;

import com.google.samples.apps.iosched.framework.Model;
import com.google.samples.apps.iosched.framework.QueryEnum;
import com.google.samples.apps.iosched.framework.UserActionEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Created by kkho on 05.05.2016.
 */
public class MyScheduleModel implements Model {

    protected final static String TAG = makeLogTag(MyScheduleModel.class);

    private final Context mContext;
    private Uri mBlockUri;

    public MyScheduleModel(Uri blockUri, Context context) {
        mContext = context;
        mBlockUri = blockUri;
    }

    @Override
    public QueryEnum[] getQueries() {
        return new QueryEnum[0];
    }

    @Override
    public boolean readDataFromCursor(Cursor cursor, QueryEnum query) {
        return false;
    }

    @Override
    public Loader<Cursor> createCursorLoader(int loaderId, Uri uri, Bundle args) {
        return null;
    }

    @Override
    public boolean requestModelUpdate(UserActionEnum action, @Nullable Bundle args) {
        return false;
    }

    private interface BlocksQuery {

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Blocks.BLOCK_ID,
                ScheduleContract.Blocks.BLOCK_TITLE,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
                ScheduleContract.Blocks.BLOCK_TYPE,
                ScheduleContract.Blocks.BLOCK_META,
                ScheduleContract.Blocks.SESSIONS_COUNT,
                ScheduleContract.Blocks.NUM_STARRED_SESSIONS,
                ScheduleContract.Blocks.STARRED_SESSION_ID,
                ScheduleContract.Blocks.STARRED_SESSION_TITLE,
                ScheduleContract.Blocks.STARRED_SESSION_ROOM_NAME,
                ScheduleContract.Blocks.STARRED_SESSION_ROOM_ID,
                ScheduleContract.Blocks.STARRED_SESSION_HASHTAGS,
                ScheduleContract.Blocks.STARRED_SESSION_URL,
                ScheduleContract.Blocks.STARRED_SESSION_LIVESTREAM_URL,
        };

        int _ID = 0;
        int BLOCK_ID = 1;
        int BLOCK_TITLE = 2;
        int BLOCK_START = 3;
        int BLOCK_END = 4;
        int BLOCK_TYPE = 5;
        int BLOCK_META = 6;
        int SESSIONS_COUNT = 7;
        int NUM_STARRED_SESSIONS = 8;
        int STARRED_SESSION_ID = 9;
        int STARRED_SESSION_TITLE = 10;
        int STARRED_SESSION_ROOM_NAME = 11;
        int STARRED_SESSION_ROOM_ID = 12;
        int STARRED_SESSION_HASHTAGS = 13;
        int STARRED_SESSION_URL = 14;
        int STARRED_SESSION_LIVESTREAM_URL = 15;
    }
}