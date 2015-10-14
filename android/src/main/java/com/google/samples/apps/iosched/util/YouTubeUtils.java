/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.util;

import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeIntents;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.google.samples.apps.iosched.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Utility class to deal with YouTube urls and videos.
 */
public class YouTubeUtils {
    private static final String TAG = makeLogTag(YouTubeUtils.class);

    public static String getYouTubeIdFromIdOrUrl(String idOrUrl) {
        Uri uri = Uri.parse(idOrUrl);
        if (uri.getScheme() != null) {
            return uri.getLastPathSegment();
        } else {
            return idOrUrl;
        }
    }

    /**
     *
     * Builds and returns the youTube video ID for a session. For livestreamed sessions, uses the
     * livestream ID only if a youTube ID isn't yet available.
     *
     * @param youTubeUrl  The ID for the youTube link to the session video.
     * @param liveStreamId The ID for the liveStream link.
     * @return The ID used for the video link for the session.
     */
    public static String getVideoIdFromSessionData(String youTubeUrl, String liveStreamId) {
        String videoId = null;
        if (!TextUtils.isEmpty(youTubeUrl)) {
            String url = youTubeUrl;
            videoId = YouTubeUtils.getYouTubeIdFromIdOrUrl(url);
        } else if (!TextUtils.isEmpty(liveStreamId)) {
            String url = liveStreamId;
            videoId = YouTubeUtils.getYouTubeIdFromIdOrUrl(url);
        }
        return videoId;
    }

    public static void showYouTubeVideo(String videoId, Activity activity) {
        if (!TextUtils.isEmpty(videoId)) {
            Intent liveIntent;
            if (YouTubeIntents.isYouTubeInstalled(activity) && YouTubeApiServiceUtil
                    .isYouTubeApiServiceAvailable(activity)
                    == YouTubeInitializationResult.SUCCESS) {
                // YouTube service is available.
                LOGW(TAG, "YouTube service available.");
                // start the YouTube player
                liveIntent = YouTubeStandalonePlayer.createVideoIntent(activity,
                        com.google.samples.apps.iosched.BuildConfig.YOUTUBE_API_KEY, videoId);
            } else if (YouTubeIntents.canResolvePlayVideoIntent(activity)) {
                // The YouTube app may not be fully up-to-date but it is installed and can resolve
                // intents.
                LOGW(TAG, "YouTube can resolve the intent.");
                // Start an intent to the YouTube app
                liveIntent = YouTubeIntents.createPlayVideoIntent(activity, videoId);
            } else {
                // YouTube may not be installed or it may be disabled.
                LOGW(TAG, "Redirecting to a browser.");
                liveIntent = new Intent(Intent.ACTION_VIEW);
                liveIntent.setData(Uri.parse("https://www.youtube.com/watch?v=" + videoId));
            }
            activity.startActivity(liveIntent);
        } else {
            Toast.makeText(activity, R.string.explore_io_video_id_not_valid,
                    Toast.LENGTH_LONG).show();
        }
    }
}
