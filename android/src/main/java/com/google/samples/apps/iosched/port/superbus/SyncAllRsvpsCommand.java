package com.google.samples.apps.iosched.port.superbus;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.samples.apps.iosched.appwidget.ScheduleWidgetProvider;
import com.google.samples.apps.iosched.port.tasks.AllRsvpsRequest;
import com.google.samples.apps.iosched.port.tasks.DataHelper;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.sync.userdata.util.UserDataHelper;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import co.touchlab.android.superbus.CheckedCommand;
import co.touchlab.android.superbus.Command;
import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;
import retrofit.RestAdapter;
import retrofit.client.Response;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

/**
 * Created by kgalligan on 8/30/14.
 */
public class SyncAllRsvpsCommand extends CheckedCommand
{
    private String accountName;

    public SyncAllRsvpsCommand(String accountName)
    {
        this.accountName = accountName;
    }

    public SyncAllRsvpsCommand()
    {
    }

    @Override
    public boolean handlePermanentError(@NotNull Context context, @NotNull PermanentException exception)
    {
        return true;//Not true
    }

    @Override
    public String logSummary()
    {
        return "Sync all";
    }

    @Override
    public boolean same(@NotNull Command command)
    {
        return command instanceof SyncAllRsvpsCommand;
    }

    @Override
    public void callCommand(@NotNull Context context) throws TransientException, PermanentException
    {
        Set<String> remote = UserDataHelper.fromString(fetchRemote(context));
        UserDataHelper.setLocalStarredSessions(context, remote, accountName);
        notifyContent(context);
    }

    private String fetchRemote(Context context) throws PermanentException, TransientException
    {
        try
        {
            RestAdapter build = DataHelper.makeRequestAdapterBuilder(context).build();
            Response response = build.create(AllRsvpsRequest.class).allRsvps();

            InputStream in = response.getBody().in();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int read;
            byte[] buf = new byte[1024];

            while ((read = in.read(buf)) > -1)
            {
                bos.write(buf, 0, read);
            }

            String json = new String(bos.toByteArray());

            Log.v(SyncAllRsvpsCommand.class.getSimpleName(), "Got this content from remote myschedule: [" + json + "]");
            return json;
        }
        catch (IOException e)
        {
            throw new TransientException(e);
        }
    }

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
}
