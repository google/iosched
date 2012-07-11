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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Temporarily just a stub.
 */
public class YouTubePlayerSupportFragment extends Fragment implements YouTubePlayer {
    public YouTubePlayerSupportFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = new View(getActivity());
        v.setBackgroundColor(0);
        return v;
    }

    @Override
    public void cueVideo(String s) {
    }

    @Override
    public void loadVideo(String s) {
    }

    @Override
    public void cuePlaylist(String s) {
    }

    @Override
    public void loadPlaylist(String s) {
    }

    @Override
    public void cueVideos(List<String> strings) {
    }

    @Override
    public void loadVideos(List<String> strings) {
    }

    @Override
    public void play() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void release() {
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasPrevious() {
        return false;
    }

    @Override
    public void next() {
    }

    @Override
    public void previous() {
    }

    @Override
    public int getCurrentTimeMillis() {
        return 0;
    }

    @Override
    public void seekToMillis(int i) {
    }

    @Override
    public void seekRelativeMillis(int i) {
    }

    @Override
    public void setFullscreen(boolean b) {
    }

    @Override
    public void enableCustomFullscreen(OnFullscreenListener onFullscreenListener) {
    }

    @Override
    public void setFullscreenControlFlags(int i) {
    }

    @Override
    public void setShowControls(boolean b) {
    }

    @Override
    public void setManageAudioFocus(boolean b) {
    }

    @Override
    public void setOnPlaybackEventsListener(OnPlaybackEventsListener onPlaybackEventsListener) {
    }

    @Override
    public void setUseSurfaceTexture(boolean b) {
    }
}
