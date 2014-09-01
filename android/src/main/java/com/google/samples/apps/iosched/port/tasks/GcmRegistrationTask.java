package com.google.samples.apps.iosched.port.tasks;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.gcm.ServerUtilities;
import com.google.samples.apps.iosched.util.AccountUtils;

import java.io.IOException;

import co.touchlab.android.threading.tasks.TaskQueue;

/**
 * Created by kgalligan on 8/31/14.
 */
public class GcmRegistrationTask extends TaskQueue.Task
{
    @Override
    protected void run(Context context) throws Exception
    {
        final String gcmKey = findGcmKey(context);
        if(ServerUtilities.isRegisteredOnServer(context, gcmKey))
            return;

        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        if(gcm == null)
        {
            Log.e(GcmRegistrationTask.class.getSimpleName(), "No gcm :(");
            return;
        }
        String regid = getRegistrationId(context);

        if (regid.isEmpty()) {
            registerInBackground(context, gcm, gcmKey);
        }
    }

    private String findGcmKey(Context context)
    {
        return AccountUtils.hasActiveAccount(context) ? AccountUtils.getGcmKey(context, AccountUtils.getActiveAccountName(context)) : null;
    }

    @Override
    protected boolean handleError(Exception e)
    {
        return true;
    }

    private void registerInBackground(Context context, GoogleCloudMessaging gcm, String gcmKey)
    {
        try {

            String regid = gcm.register(Config.GCM_SENDER_ID);

            // You should send the registration ID to your server over HTTP,
            // so it can use GCM/HTTP or CCS to send messages to your app.
            // The request to your server should be authenticated if your app
            // is using accounts.
            ServerUtilities.registerFromTask(context, regid, gcmKey, getAppVersion(context));
        } catch (IOException ex) {
            //TODO: No idea
        }
    }

    private String getRegistrationId(Context context) {
        AppPrefs appPrefs = AppPrefs.getInstance(context);
        String registrationId = appPrefs.getGcmRegistrationId();
        if (registrationId.isEmpty()) {
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = appPrefs.getGcmAppVersion();
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            return "";
        }
        return registrationId;
    }

    public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }
}
