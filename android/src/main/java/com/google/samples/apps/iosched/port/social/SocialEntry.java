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
    public String entryUrl;
    public int replies = 0;
    public int plusOnes = 0;
    public int shares = 0;

    public enum SocialType
    {
        Twitter, Google
    }
}
