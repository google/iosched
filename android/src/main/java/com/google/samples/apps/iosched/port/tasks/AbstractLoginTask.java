package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;

import co.touchlab.android.threading.tasks.TaskQueue;

/**
 * Created by kgalligan on 8/17/14.
 */
public class AbstractLoginTask extends TaskQueue.Task
{
    @Override
    protected void run(Context context) throws Exception {

    }

    @Override
    protected boolean handleError(Exception e) {
        return false;
    }

    boolean firstLogin;


}
