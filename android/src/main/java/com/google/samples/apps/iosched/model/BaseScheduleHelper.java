package com.google.samples.apps.iosched.model;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.MyScheduleAdapter;
import com.google.samples.apps.iosched.util.PrefUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import co.touchlab.droidconnyc.BuildConfig;
import co.touchlab.droidconnyc.R;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Created by kgalligan on 8/11/14.
 */
public abstract class BaseScheduleHelper {

    private static final String TAG = makeLogTag(BaseScheduleHelper.class);

    protected Context mContext;

    public BaseScheduleHelper(Context mContext) {
        this.mContext = mContext;
    }

    abstract protected void addSessions(long start, long end, ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems);

    abstract boolean shouldCheckConflicts();

    public ArrayList<ScheduleItem> getScheduleData(long start, long end) {
        // get sessions in my schedule and blocks, starting anytime in the conference day
        ArrayList<ScheduleItem> mutableItems = new ArrayList<ScheduleItem>();
        ArrayList<ScheduleItem> immutableItems = new ArrayList<ScheduleItem>();
        addBlocks(start, end, mutableItems, immutableItems);
        addSessions(start, end, mutableItems, immutableItems);

        ArrayList<ScheduleItem> result = ScheduleItemHelper.processItems(mutableItems, immutableItems, shouldCheckConflicts());
        if (BuildConfig.DEBUG || Log.isLoggable(TAG, Log.DEBUG)) {
            ScheduleItem previous = null;
            for (ScheduleItem item : result) {
                if ((item.flags & ScheduleItem.FLAG_CONFLICTS_WITH_PREVIOUS) != 0) {
                    Log.d(TAG, "Schedule Item conflicts with previous. item=" + item + " previous=" + previous);
                }
                previous = item;
            }
        }

        setSessionCounters(result, start, end);
        return result;
    }

    public void getScheduleDataAsync(final MyScheduleAdapter adapter,
                                     long start, long end) {
        new AsyncTask<Long, Void, ArrayList<ScheduleItem>>() {
            @Override
            protected ArrayList<ScheduleItem> doInBackground(Long... params) {
                Long start = params[0];
                Long end = params[1];
                return getScheduleData(start, end);
            }

            @Override
            protected void onPostExecute(ArrayList<ScheduleItem> scheduleItems) {
                adapter.updateItems(scheduleItems);
            }
        }.execute(start, end);
    }

    protected void addBlocks(long start, long end,
                             ArrayList<ScheduleItem> mutableItems, ArrayList<ScheduleItem> immutableItems) {
        Cursor cursor = mContext.getContentResolver().query(
                ScheduleContract.Blocks.CONTENT_URI,
                BlocksQuery.PROJECTION,

                // filter sessions on the specified day
                ScheduleContract.Blocks.BLOCK_START + " >= ? and " + ScheduleContract.Blocks.BLOCK_START + " <= ?",
                new String[]{String.valueOf(start), String.valueOf(end)},

                // order by session start
                ScheduleContract.Blocks.BLOCK_START);

        while (cursor.moveToNext()) {
            ScheduleItem item = new ScheduleItem();
            item.setTypeFromBlockType(cursor.getString(BlocksQuery.BLOCK_TYPE));
            item.title = cursor.getString(BlocksQuery.BLOCK_TITLE);
            item.subtitle = cursor.getString(BlocksQuery.BLOCK_SUBTITLE);
            item.startTime = cursor.getLong(BlocksQuery.BLOCK_START);
            item.endTime = cursor.getLong(BlocksQuery.BLOCK_END);

            // Hide BREAK blocks to remote attendees (b/14666391):
            if (item.type == ScheduleItem.BREAK && !PrefUtils.isAttendeeAtVenue(mContext)) {
                continue;
            }
            // Currently, only type=FREE is mutable
            if (item.type == ScheduleItem.FREE) {
                mutableItems.add(item);
            } else {
                immutableItems.add(item);
                item.flags |= ScheduleItem.FLAG_NOT_REMOVABLE;
            }
        }
    }

    /**
     * Fill the number of sessions for FREE blocks:
     */
    protected void setSessionCounters(ArrayList<ScheduleItem> items, long dayStart, long dayEnd) {
        ArrayList<ScheduleItem> free = new ArrayList<ScheduleItem>();

        for (ScheduleItem item : items) {
            if (item.type == ScheduleItem.FREE) {
                free.add(item);
            }
        }

        if (free.isEmpty()) {
            return;
        }

        // Count number of start/end pairs for sessions that are between dayStart and dayEnd and
        // are not in my schedule:
        String liveStreamedOnlySelection = UIUtils.shouldShowLiveSessionsOnly(mContext)
                ? "AND IFNULL(" + ScheduleContract.Sessions.SESSION_LIVESTREAM_URL + ",'')!=''"
                : "";
        Cursor cursor = mContext.getContentResolver().query(
                ScheduleContract.Sessions.buildCounterByIntervalUri(),
                SessionsCounterQuery.PROJECTION,
                ScheduleContract.Sessions.SESSION_START + ">=? AND " + ScheduleContract.Sessions.SESSION_START + "<=? AND " +
                        ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE + " = 0 " + liveStreamedOnlySelection,
                new String[]{String.valueOf(dayStart), String.valueOf(dayEnd)},
                null);

        while (cursor.moveToNext()) {
            long start = cursor.getLong(SessionsCounterQuery.SESSION_INTERVAL_START);
            int counter = cursor.getInt(SessionsCounterQuery.SESSION_INTERVAL_COUNT);

            // Find blocks that this interval applies.
            for (ScheduleItem item : free) {
                // If grouped sessions starts and ends inside the free block, it is considered in it:
                if (item.startTime <= start && start < item.endTime) {
                    item.numOfSessions += counter;
                }
            }
        }
        cursor.close();

        // remove free blocks that have no available sessions or that are in the past
        long now = UIUtils.getCurrentTime(mContext);
        Iterator<ScheduleItem> it = items.iterator();
        while (it.hasNext()) {
            ScheduleItem i = it.next();
            if (i.type == ScheduleItem.FREE) {
                if (i.endTime < now) {
                    LOGD(TAG, "Removing empty block in the past.");
                    it.remove();
                } else if (i.numOfSessions == 0) {
                    LOGD(TAG, "Removing block with zero sessions: " + new Date(i.startTime) + "-" + new Date(i.endTime));
                    it.remove();
                } else {
                    i.subtitle = mContext.getResources().getQuantityString(
                            R.plurals.schedule_block_subtitle, i.numOfSessions, i.numOfSessions);
                }

            }
        }
    }

    private interface BlocksQuery {
        String[] PROJECTION = {
                ScheduleContract.Blocks.BLOCK_TITLE,
                ScheduleContract.Blocks.BLOCK_TYPE,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
                ScheduleContract.Blocks.BLOCK_SUBTITLE
        };

        int BLOCK_TITLE = 0;
        int BLOCK_TYPE = 1;
        int BLOCK_START = 2;
        int BLOCK_END = 3;
        int BLOCK_SUBTITLE = 4;
    }

    private interface SessionsCounterQuery {
        String[] PROJECTION = {
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Sessions.SESSION_INTERVAL_COUNT,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
        };

        int SESSION_INTERVAL_START = 0;
        int SESSION_INTERVAL_END = 1;
        int SESSION_INTERVAL_COUNT = 2;
    }


}
