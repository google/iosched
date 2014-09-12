package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.google.samples.apps.iosched.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

import co.touchlab.android.threading.eventbus.EventBusExt;
import co.touchlab.android.threading.tasks.TaskQueue;
import co.touchlab.droidconnyc.BuildConfig;
import retrofit.client.Response;

/**
 * Created by kgalligan on 9/12/14.
 */
public class CheckTicketTask extends TaskQueue.Task
{
    public boolean newRequest;
    public String ticketStatus;

    @Override
    protected void run(Context context) throws Exception
    {
        if(BuildConfig.DEBUG)
        {
            AppPrefs appPrefs = AppPrefs.getInstance(context);
            ticketStatus = appPrefs.getTicketStatus();
            if (ticketStatus == null)
            {
                newRequest = true;

                Response response = DataHelper.makeRequestAdapter(context).create(CheckTicketRequest.class).checkTicket();
                String json = IOUtils.toString(response.getBody().in());
                appPrefs.setTicketStatus(json);
                ticketStatus = json;
            }

            EventBusExt.getDefault().post(this);
        }
    }

    @Override
    protected boolean handleError(Exception e)
    {
        //If you're reading this, no, this isn't what I'd normally do.  However,
        //time is short...
        return true;
    }


}
