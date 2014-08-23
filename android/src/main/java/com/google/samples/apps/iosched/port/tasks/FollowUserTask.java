package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;

import co.touchlab.android.threading.tasks.TaskQueue;
import retrofit.RestAdapter;

/**
 * Created by kgalligan on 8/23/14.
 */
public class FollowUserTask extends TaskQueue.Task
{
    private Long userId;
    private boolean follow;

    public FollowUserTask(Long userId, boolean follow)
    {
        this.userId = userId;
        this.follow = follow;
    }

    @Override
    protected void run(Context context) throws Exception
    {
        RestAdapter restAdapter = DataHelper.makeRequestAdapter(context);
        FollowUserRequest request = restAdapter.create(FollowUserRequest.class);

        if(follow)
            request.follow(userId);
        else
            request.unfollow(userId);
    }

    @Override
    protected boolean handleError(Exception e)
    {
        return true;
    }
}
