package com.google.samples.apps.iosched.port.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Created by kgalligan on 9/1/14.
 */
public class PackageHelper
{

    public static final String TWITTER_PACKAGE = "com.twitter.android";

    public boolean packageInstalled(Activity activity, String targetPackage)
    {
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = activity.getPackageManager();
        packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages)
        {
            if (packageInfo.packageName.startsWith(targetPackage)) return true;
        }
        return false;
    }

    public static ActivityInfo findSharePackage(Activity activity, String packageName)
    {
        Intent tweetIntent = new Intent(Intent.ACTION_SEND);
        tweetIntent.putExtra(Intent.EXTRA_TEXT, "This is a Test.");
        tweetIntent.setType("text/plain");

        PackageManager packManager = activity.getPackageManager();
        List<ResolveInfo> resolvedInfoList = packManager.queryIntentActivities(tweetIntent,  PackageManager.MATCH_DEFAULT_ONLY);

        for(ResolveInfo resolveInfo: resolvedInfoList){
            if(resolveInfo.activityInfo.packageName.startsWith(packageName)){
                return resolveInfo.activityInfo;
            }
        }

        return null;
    }

    public static boolean twitterInstalled(Activity activity)
    {
        return findTwitterPackage(activity) != null;
    }

    public static ActivityInfo findTwitterPackage(Activity activity)
    {
        return findSharePackage(activity, TWITTER_PACKAGE);
    }
}
