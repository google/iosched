package com.google.samples.apps.iosched.port.tasks;

import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;

/**
 * Created by kgalligan on 7/20/14.
 */
public interface FollowUserRequest
{
    @POST("/dataTest/follow")
    LoginResult follow(@Field("otherId") Long otherId) throws TransientException, PermanentException;

    @POST("/dataTest/unfollow")
    LoginResult unfollow(@Field("otherId") Long otherId) throws TransientException, PermanentException;
}
