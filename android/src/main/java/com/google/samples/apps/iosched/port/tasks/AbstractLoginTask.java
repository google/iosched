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

    void handleLoginResult(Context context, LoginResult loginResult)
    {
        UserAuthHelper.processLoginResonse(context, loginResult);
//        firstLogin = StringUtils.isEmpty(userAccount.profile) && StringUtils.isEmpty(userAccount.company)
//
//        CommandBusHelper.submitCommandSync(context, RefreshScheduleDataKot())
    }
}
