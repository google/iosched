package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;

import co.touchlab.android.threading.eventbus.EventBusExt;
import co.touchlab.android.threading.tasks.TaskQueue;
import co.touchlab.droidconandroid.network.dao.UserInfoResponse;
import retrofit.RestAdapter;

/**
 * Created by kgalligan on 8/23/14.
 */
public class FindUserByCodeTask extends TaskQueue.Task
{
    private String code;
    private UserInfoResponse userInfoResponse;

    public FindUserByCodeTask(String code)
    {
        this.code = code;
    }

    public UserInfoResponse getUserInfoResponse()
    {
        return userInfoResponse;
    }

    @Override
    protected void run(Context context) throws Exception
    {
        RestAdapter restAdapter = DataHelper.makeRequestAdapter(context);
        FindUserByCode findUserByCode= restAdapter.create(FindUserByCode.class);
        try
        {
            userInfoResponse = findUserByCode.findUserByCode(code);
        }
        catch (Exception e)
        {
            //Ehh
        }
    }

    @Override
    protected boolean handleError(Exception e)
    {
        return false;
    }

    @Override
    protected void onComplete()
    {
        EventBusExt.getDefault().post(this);
    }
}
