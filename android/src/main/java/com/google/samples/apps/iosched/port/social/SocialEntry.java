package com.google.samples.apps.iosched.port.social;

import java.util.Date;

/**
 * Created by kgalligan on 9/1/14.
 */
public class SocialEntry
{
    public Long id;

    public Date createdAt;

    public SocialType socialType;

    public Long sourceId;
    public String textVal;
    public String screenName;
    public String username;
    public String profileImage;

    public enum SocialType
    {
        Twitter
    }
}
