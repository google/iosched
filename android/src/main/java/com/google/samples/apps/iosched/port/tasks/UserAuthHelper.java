package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;

/**
 * Created by kgalligan on 8/17/14.
 */
public class UserAuthHelper {
    public static void processLoginResonse(Context c, LoginResult loginResult)
    {
//        val newDbUser = UserAccount()
//        userAccountToDb(result.user, newDbUser)
//        DatabaseHelper.getInstance(c).getUserAccountDao().createOrUpdate(newDbUser)

        //Save db first, then these.
        AppPrefs appPrefs = AppPrefs.getInstance(c);
        appPrefs.setUserUuid(loginResult.uuid);
        appPrefs.setUserId(loginResult.userId);

//        CommandBusHelper.submitCommandSync(c, RefreshScheduleDataKot())
//
//        return newDbUser
    }
}
