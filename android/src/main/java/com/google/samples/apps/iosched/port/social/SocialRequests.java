package com.google.samples.apps.iosched.port.social;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Path;

/**
 * Created by kgalligan on 9/1/14.
 */
public interface SocialRequests
{
    @GET("/dataTest/twitterSearch")
    List<SocialEntry> socialFeed();
}
