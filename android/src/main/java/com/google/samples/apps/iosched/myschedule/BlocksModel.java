package com.google.samples.apps.iosched.myschedule;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;
import com.google.samples.apps.iosched.framework.Model;
import com.google.samples.apps.iosched.framework.QueryEnum;
import com.google.samples.apps.iosched.framework.UserActionEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;

/**
 * Created by kkho on 06.05.2016.
 */
public class BlocksModel implements Model {
    private final Context mContext;

    private String mBlockId;
    private String mBlockTitle;
    private long mBlockStart;
    private long mBlockEnd;
    private String mBlockSubTitle;
    private String mBlockMeta;

    private int mSessionsCount;
    private int mNumStarredSessions;
    private String mStarredSessionId;
    private String mStarredSessionTitle;
    private String mStarredSessionRoomName;
    private String mStarredSessionRoomId;
    private String mStarredHashTags;
    private String mStarredSessionUrl;
    private String mStarredSessionLiveStreamUrl;

    public BlocksModel


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
        CursorLoader loader = null;
        if(loaderId == BlocksQueryEnum.BLOCKS.getId()) {
            loader = getCursorLoaderInstance(mContext, uri,
                    BlocksQueryEnum.BLOCKS.getProjection(), null, null, null);
        }

        return loader;
    }

    @VisibleForTesting
    public CursorLoader getCursorLoaderInstance(Context context, Uri uri, String[] projection,
                                                String selection, String[] selectionArgs, String sortOrder) {
        return new CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public boolean requestModelUpdate(UserActionEnum action, @Nullable Bundle args) {
        return true;
    }

    public String getBlockId() {
        return mBlockId;
    }

    public String getBlockTitle() {
        return mBlockTitle;
    }

    public long getBlockStart() {
        return mBlockStart;
    }

    public long getBlockEnd() {
        return mBlockEnd;
    }

    public String getBlockSubTitle() {
        return mBlockSubTitle;
    }

    public String getBlockMeta() {
        return mBlockMeta;
    }

    public int getSessionsCount() {
        return mSessionsCount;
    }

    public int getNumStarredSessions() {
        return mNumStarredSessions;
    }

    public String getStarredSessionId() {
        return mStarredSessionId;
    }

    public String getStarredSessionTitle() {
        return mStarredSessionTitle;
    }

    public String getStarredSessionRoomName() {
        return mStarredSessionRoomName;
    }

    public String getStarredSessionRoomId() {
        return mStarredSessionRoomId;
    }

    public String getStarredHashTags() {
        return mStarredHashTags;
    }

    public String getStarredSessionUrl() {
        return mStarredSessionUrl;
    }

    public String getStarredSessionLiveStreamUrl() {
        return mStarredSessionLiveStreamUrl;
    }

    public static enum BlocksQueryEnum implements QueryEnum {
        BLOCKS(0x1, new String[] {
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
        });



        private int id;
        private String[] projection;

        BlocksQueryEnum(int id, String[] projection) {
            this.id = id;
            this.projection = projection;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String[] getProjection() {
            return projection;
        }
    }

}
