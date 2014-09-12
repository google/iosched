package com.google.samples.apps.iosched.port.tasks;

import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by kgalligan on 9/12/14.
 */
public interface CheckTicketRequest
{
    @GET("/dataTest/metalTicket")
    Response checkTicket();
}
