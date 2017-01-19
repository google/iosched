/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.testutils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeIntents;
import com.google.android.youtube.player.YouTubeStandalonePlayer;
import com.google.samples.apps.iosched.Config;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;

import java.util.Locale;

import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;

public class IntentUtils {

    /**
     * Check the intent to launch the video with {@code videoId} is fired. Depending on whether
     * YouTube is installed and available, a different intent is launched, unless {@code forceWeb}
     * is true (for use in features where the app always launches the youtube url, eg {@link
     * com.google.samples.apps.iosched .videolibrary .VideoLibraryActivity}).
     */
    public static void checkVideoIntentIsFired(String videoId, Activity activity,
            boolean forceWeb) {
        if (!forceWeb && YouTubeIntents.isYouTubeInstalled(activity) && YouTubeApiServiceUtil
                .isYouTubeApiServiceAvailable(activity) == YouTubeInitializationResult.SUCCESS) {
            Intent expectedIntent = YouTubeStandalonePlayer.createVideoIntent(activity,
                    com.google.samples.apps.iosched.BuildConfig.YOUTUBE_API_KEY, videoId);
            intended(CoreMatchers.allOf(
                    hasAction(expectedIntent.getAction()),
                    hasExtra("video_id", videoId)));
        } else if (!forceWeb && YouTubeIntents.canResolvePlayVideoIntent(activity)) {
            Intent expectedIntent = YouTubeIntents.createPlayVideoIntent(activity, videoId);
            intended(CoreMatchers.allOf(
                    hasAction(expectedIntent.getAction()),
                    hasExtra("video_id", videoId)));
        } else {
            Uri expectedVideoUri = Uri.parse(String.format(Locale.US, Config.VIDEO_LIBRARY_URL_FMT,
                    videoId));
            intended(CoreMatchers.allOf(
                    hasAction(IsEqual.equalTo(Intent.ACTION_VIEW)),
                    hasData(expectedVideoUri)));
        }
    }

}
