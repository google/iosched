package com.google.samples.apps.iosched.port.superbus;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.service.SessionCalendarService;
import com.google.samples.apps.iosched.sync.ConferenceDataHandler;
import com.google.samples.apps.iosched.sync.RemoteConferenceDataFetcher;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.PrefUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import co.touchlab.android.superbus.CheckedCommand;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;

/**
 * Created by kgalligan on 9/1/14.
 */
public class SyncConferenceDataCommand extends CheckedCommand
{
    public static final String TAG = SyncConferenceDataCommand.class.getSimpleName();
    @Override
    public boolean handlePermanentError(@NotNull Context context, @NotNull PermanentException exception)
    {
        //TODO: Do something
        return true;
    }

    @Override
    public String logSummary()
    {
        return SyncConferenceDataCommand.class.getSimpleName();
    }

    @Override
    public boolean same(@NotNull Command command)
    {
        return command instanceof SyncConferenceDataCommand;
    }

    @Override
    public void callCommand(@NotNull Context context) throws TransientException, PermanentException
    {
        RemoteConferenceDataFetcher mRemoteDataFetcher = new RemoteConferenceDataFetcher(context);
        ConferenceDataHandler mConferenceDataHandler = new ConferenceDataHandler(context);
        // Fetch the remote data files via RemoteConferenceDataFetcher
        String[] dataFiles;
        try
        {
            dataFiles = mRemoteDataFetcher.fetchConferenceDataIfNewer(mConferenceDataHandler.getDataTimestamp());
        }
        catch (IOException e)
        {
            throw new TransientException(e);
        }

        if (dataFiles != null) {
            LOGI(TAG, "Applying remote data.");
            // save the remote data to the database
            try
            {
                mConferenceDataHandler.applyConferenceData(dataFiles, mRemoteDataFetcher.getServerDataTimestamp());
            }
            catch (IOException e)
            {
                throw new PermanentException("Couldn't save sync data", e);
            }
            LOGI(TAG, "Done applying remote data.");

            // mark that conference data sync succeeded
            PrefUtils.markSyncSucceededNow(context);
//            return true;
        } else {
            // no data to process (everything is up to date)

            // mark that conference data sync succeeded
            PrefUtils.markSyncSucceededNow(context);
//            return false;
        }

        performPostSyncChores(context);

        String activeAccountName = AccountUtils.getActiveAccountName(context);
        if(activeAccountName != null)
        {
            Account account = new Account(activeAccountName, GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
            updateSyncInterval(context, account);
        }
    }

    public static void performPostSyncChores(final Context context) {
        // Update search index
        LOGD(TAG, "Updating search index.");
        context.getContentResolver().update(ScheduleContract.SearchIndex.CONTENT_URI,
                new ContentValues(), null, null);

        // Sync calendars
        LOGD(TAG, "Session data changed. Syncing starred sessions with Calendar.");
        syncCalendar(context);
    }

    private static void syncCalendar(Context context) {
        Intent intent = new Intent(SessionCalendarService.ACTION_UPDATE_ALL_SESSIONS_CALENDAR);
        intent.setClass(context, SessionCalendarService.class);
        context.startService(intent);
    }

    public static long calculateRecommendedSyncInterval(final Context context) {
        long now = UIUtils.getCurrentTime(context);
        long aroundConferenceStart = Config.CONFERENCE_START_MILLIS - Config.AUTO_SYNC_AROUND_CONFERENCE_THRESH;
        if (now < aroundConferenceStart) {
            return Config.AUTO_SYNC_INTERVAL_LONG_BEFORE_CONFERENCE;
        } else if (now <= Config.CONFERENCE_END_MILLIS) {
            return Config.AUTO_SYNC_INTERVAL_AROUND_CONFERENCE;
        } else {
            return Config.AUTO_SYNC_INTERVAL_AFTER_CONFERENCE;
        }
    }

    public static void updateSyncInterval(final Context context, final Account account) {
        LOGD(TAG, "Checking sync interval for " + account);
        long recommended = calculateRecommendedSyncInterval(context);
        long current = PrefUtils.getCurSyncInterval(context);
        LOGD(TAG, "Recommended sync interval " + recommended + ", current " + current);
        if (recommended != current) {
            LOGD(TAG, "Setting up sync for account " + account + ", interval " + recommended + "ms");
            ContentResolver.setIsSyncable(account, ScheduleContract.CONTENT_AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, ScheduleContract.CONTENT_AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, ScheduleContract.CONTENT_AUTHORITY,
                    new Bundle(), recommended / 1000L);
            PrefUtils.setCurSyncInterval(context, recommended);
        } else {
            LOGD(TAG, "No need to update sync interval.");
        }
    }
}
