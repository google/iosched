package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import co.touchlab.android.threading.eventbus.EventBusExt;
import co.touchlab.android.threading.tasks.TaskQueue;
import co.touchlab.droidconandroid.network.dao.UserFollowResponse;
import retrofit.RestAdapter;
import retrofit.client.Response;

/**
 * Created by kgalligan on 8/23/14.
 */
public class FindAllFollowsTask extends TaskQueue.Task
{
    private UserFollowResponse followResponse;

    public UserFollowResponse getFollowResponse()
    {
        return followResponse;
    }

    @Override
    protected void run(Context context) throws Exception
    {
        RestAdapter restAdapter = DataHelper.makeRequestAdapter(context);
        FollowUserRequest followUserRequest= restAdapter.create(FollowUserRequest.class);
        followResponse = followUserRequest.allFollowedUsers();
    }

    private void debugOut(Response response) throws IOException
    {
        InputStream in = response.getBody().in();
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read;
        while((read = in.read(buf)) > 0)
        {
            bout.write(buf, 0, read);
        }

        String body = new String(bout.toByteArray());
        Log.w("asdf", body);
    }

    @Override
    protected void onComplete()
    {
        EventBusExt.getDefault().post(this);
    }

    @Override
    protected boolean handleError(Exception e)
    {
        return false;
    }
}
