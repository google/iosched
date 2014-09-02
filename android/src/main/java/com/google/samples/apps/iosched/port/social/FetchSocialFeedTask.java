package com.google.samples.apps.iosched.port.social;

import android.content.Context;
import android.util.Log;

import com.google.samples.apps.iosched.port.tasks.DataHelper;

import java.util.List;

import co.touchlab.android.threading.eventbus.EventBusExt;
import co.touchlab.android.threading.tasks.TaskQueue;

/**
 * Created by kgalligan on 9/1/14.
 */
public class FetchSocialFeedTask extends TaskQueue.Task
{
    public List<SocialEntry> socialEntries;

    @Override
    protected void run(Context context) throws Exception
    {
        try
        {
            socialEntries = DataHelper.makeRequestAdapter(context).create(SocialRequests.class).socialFeed();
        }
        catch (Exception e)
        {
            Log.e(FetchSocialFeedTask.class.getSimpleName(), "Failed", e);
        }

        EventBusExt.getDefault().post(this);
    }

    @Override
    protected boolean handleError(Exception e)
    {
        return true;
    }
}
