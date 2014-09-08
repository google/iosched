package com.google.samples.apps.iosched.port.tasks;

import co.touchlab.android.superbus.errorcontrol.PermanentException;
import co.touchlab.android.superbus.errorcontrol.TransientException;
import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by kgalligan on 8/31/14.
 */
public interface RsvpRequests
{
    @POST("/dataTest/rsvpEvent/{eventId}")
    BasicIdResult addRsvp(@Path("eventId") Long eventId) throws TransientException, PermanentException;

    @POST("/dataTest/sendDiscountEmail")
    Response sendDiscountEmail() throws TransientException, PermanentException;

    @POST("/dataTest/unRsvpEvent/{eventId}")
    BasicIdResult removeRsvp(@Path("eventId") Long eventId) throws TransientException, PermanentException;

    @GET("/dataTest/allRsvps")
    Response allRsvps() throws TransientException, PermanentException;
}
