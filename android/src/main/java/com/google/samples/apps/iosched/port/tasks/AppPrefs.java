package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Created by kgalligan on 8/17/14.
 */
public class AppPrefs
{
    public static final String USER_UUID = "USER_UUID";
    public static final String USER_ID = "USER_ID";
    private static AppPrefs instance;

    private SharedPreferences prefs;

    public static synchronized AppPrefs getInstance(Context context)
    {
        if (instance == null)
        {
            instance = new AppPrefs();
            instance.prefs = context.getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE);
        }

        return instance;
    }

    public boolean isLoggedIn()
    {
        return !TextUtils.isEmpty(getUserUuid());
    }

    public String getUserUuid()
    {
        return prefs.getString(USER_UUID, null);
    }

    public void setUserUuid(String uuid)
    {
        prefs.edit().putString(USER_UUID, uuid).apply();
    }

    public Long getUserId()
    {
        long id = prefs.getLong(USER_ID, -1);
        return id == -1 ? null : id;
    }

    public void setUserId(Long id)
    {
        prefs.edit().putLong(USER_ID, id).apply();
    }
}
