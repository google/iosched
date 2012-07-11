/*
 * Copyright 2012 Google Inc.
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

package com.google.android.youtube.api;

/**
 * Temporarily just a stub.
 */
public interface YouTubePlayer {

    public static int FULLSCREEN_FLAG_CONTROL_ORIENTATION = 1;
    public static int FULLSCREEN_FLAG_CONTROL_SYSTEM_UI = 2;
    public static int FULLSCREEN_FLAG_FULLSCREEN_WHEN_DEVICE_LANDSCAPE = 4;

    public void cueVideo(java.lang.String s);

    public void loadVideo(java.lang.String s);

    public void cuePlaylist(java.lang.String s);

    public void loadPlaylist(java.lang.String s);

    public void cueVideos(java.util.List<java.lang.String> strings);

    public void loadVideos(java.util.List<java.lang.String> strings);

    public void play();

    public void pause();

    public void release();

    public boolean hasNext();

    public boolean hasPrevious();

    public void next();

    public void previous();

    public int getCurrentTimeMillis();

    public void seekToMillis(int i);

    public void seekRelativeMillis(int i);

    public void setFullscreen(boolean b);

    public void enableCustomFullscreen(com.google.android.youtube.api.YouTubePlayer.OnFullscreenListener onFullscreenListener);

    public void setFullscreenControlFlags(int i);

    public void setShowControls(boolean b);

    public void setManageAudioFocus(boolean b);

    public void setOnPlaybackEventsListener(com.google.android.youtube.api.YouTubePlayer.OnPlaybackEventsListener onPlaybackEventsListener);

    public void setUseSurfaceTexture(boolean b);

    public static interface OnFullscreenListener {
        void onFullscreen(boolean b);
    }

    public static interface OnPlaybackEventsListener {
        void onLoaded(java.lang.String s);

        void onPlaying();

        void onPaused();

        void onBuffering(boolean b);

        void onEnded();

        void onError();
    }
}
