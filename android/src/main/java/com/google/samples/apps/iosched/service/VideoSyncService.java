package com.google.samples.apps.iosched.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.samples.apps.iosched.sync.SyncHelper;

import java.io.IOException;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Created by kkho on 28.05.2016.
 */
public class VideoSyncService extends IntentService {
    private static final String TAG = makeLogTag(VideoSyncService.class);

    public VideoSyncService() {
        super("VideoSyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            new SyncHelper(getApplicationContext()).syncVideos(1);
        } catch (IOException e) {
            Log.e(TAG, "exception performing sync on Videos");

        }

    }
}
