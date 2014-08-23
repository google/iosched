package com.google.samples.apps.iosched.port.tasks;

import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;
import co.touchlab.droidconandroid.network.dao.UserFollowResponse;
import retrofit.client.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;

/**
 * Created by kgalligan on 7/20/14.
 */
public interface FollowUserRequest
{
    @FormUrlEncoded
    @POST("/dataTest/follow")
    LoginResult follow(@Field("otherId") Long otherId) throws TransientException, PermanentException;

    @FormUrlEncoded
    @POST("/dataTest/unfollow")
    LoginResult unfollow(@Field("otherId") Long otherId) throws TransientException, PermanentException;

    @GET("/dataTest/allFollowedUsers")
    UserFollowResponse allFollowedUsers();
}
