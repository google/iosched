package com.google.samples.apps.iosched.port.superbus;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.samples.apps.iosched.appwidget.ScheduleWidgetProvider;
import com.google.samples.apps.iosched.port.tasks.DataHelper;
import com.google.samples.apps.iosched.port.tasks.RsvpRequests;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.service.SessionAlarmService;
import com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper;
import com.google.samples.apps.iosched.ui.MyScheduleActivity;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Set;

import co.touchlab.android.superbus.CheckedCommand;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;
import co.touchlab.droidconnyc.R;
import retrofit.RestAdapter;
import retrofit.client.Response;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

/**
 * Just pulls remote data and applies it locally.  If you dump the comments, there's very little logic
 * here, which is kind of the point.  Pushing data happens as needed.
 * @see com.google.samples.apps.iosched.port.superbus.AddRsvpCommand
 * @see com.google.samples.apps.iosched.port.superbus.RemoveRsvpCommand
 *
 * Created by kgalligan on 8/17/14.
 */
public class SyncAllRsvpsCommand extends CancellableCheckedCommand
{
    public static final String TAG = SyncAllRsvpsCommand.class.getSimpleName();
    private String accountName;

    transient Set<String> remote;

    public SyncAllRsvpsCommand(String accountName)
    {
        this.accountName = accountName;
    }

    /**
     * Empty constructor required for possibly inflating later.
     */
    public SyncAllRsvpsCommand()
    {
    }

    @Override
    public boolean handlePermanentError(@NotNull Context context, @NotNull PermanentException exception)
    {
        showError(context);
        return true;
    }

    @Override
    public String logSummary()
    {
        return "Sync all";
    }

    /**
     * Don't need (or want) 2 of these.
     * @param command
     * @return
     */
    @Override
    public boolean same(@NotNull Command command)
    {
        return command instanceof SyncAllRsvpsCommand;
    }

    @Override
    protected void setupData(@NotNull Context context) throws TransientException, PermanentException
    {
        remote = UserDataHelper.fromString(fetchRemote(context));
    }

    @Override
    protected void commitData(@NotNull Context context) throws PermanentException
    {
        UserDataHelper.setLocalStarredSessions(context, remote, accountName);
        //TODO: test updated
        updateAlarm(context, true);
        notifyContent(context);
    }

    private void updateAlarm(Context context, boolean modified)
    {
        if (modified) {
            // schedule notifications for the starred sessions
            Intent scheduleIntent = new Intent(
                    SessionAlarmService.ACTION_SCHEDULE_ALL_STARRED_BLOCKS,
                    null, context, SessionAlarmService.class);
            context.startService(scheduleIntent);
        }
    }

    private String fetchRemote(Context context) throws PermanentException, TransientException
    {
        try
        {
            RestAdapter build = DataHelper.makeRequestAdapterBuilder(context).build();
            Response response = build.create(RsvpRequests.class).allRsvps();

            String json = IOUtils.toString(response.getBody().in());

            Log.v(TAG, "Got this content from remote myschedule: [" + json + "]");

            return json;
        }
        catch (IOException e)
        {
            //If you're reading this, I don't know if this is correct.  IOException may be caused by
            //something less "transient", but we've got to wrap this project up pretty quickly. Cross fingers.
            //Specifically, does the input actually stream from the network, or is it pre cached?  If
            //streaming, we may lose connectivity (however short the timeframe may be).  If something else, bad.
            //However, if the data is already local, IOException should never happen, so a wash I guess?
            throw new TransientException(e);
        }
    }

    /**
     * Feels complex, yes?  Kind of don't dig ContentProviders for a number of reasons.  Here's one.
     * @param context
     */
    private void notifyContent(Context context)
    {
        LOGD(SyncAllRsvpsCommand.class.getSimpleName(), "Notifying changes on paths related to user data on Content Resolver.");
        ContentResolver resolver = context.getContentResolver();
        for (String path : ScheduleContract.USER_DATA_RELATED_PATHS) {
            Uri uri = ScheduleContract.BASE_CONTENT_URI.buildUpon().appendPath(path).build();
            resolver.notifyChange(uri, null);
        }
        context.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(context, false));
    }


    private void showError(Context context)
    {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(0, new NotificationCompat.Builder(context)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(R.drawable.ic_stat_notification)
                        .setTicker("Sync error")
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText("Couldn't sync rsvp data. Pull to refresh in app")
                                //.setColor(context.getResources().getColor(R.color.theme_primary))
                                // Note: setColor() is available in the support lib v21+.
                                // We commented it out because we want the source to compile
                                // against support lib v20. If you are using support lib
                                // v21 or above on Android L, uncomment this line.
                        .setContentIntent(
                                PendingIntent.getActivity(context, 0,
                                        new Intent(context, MyScheduleActivity.class)
                                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                                        Intent.FLAG_ACTIVITY_SINGLE_TOP),
                                        0))
                        .setAutoCancel(true)
                        .build());
    }
}
