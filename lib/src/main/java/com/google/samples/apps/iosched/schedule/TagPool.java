/*
 * Copyright (c) 2017 Google Inc.
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

package com.google.samples.apps.iosched.schedule;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple pool of views that can be reused.
 */
public class TagPool {

    private final List<TextView> tagPool = new ArrayList<>();
    private final List<View> spacerPool = new ArrayList<>();
    private final List<ImageView> livestreamPool = new ArrayList<>();

    @CheckResult public TextView getTag(@NonNull ViewGroup parent) {
        if (tagPool.size() > 0) {
            return tagPool.remove(0);
        }
        return createTag(parent);
    }

    private TextView createTag(ViewGroup parent) {
        return (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.include_schedule_tag, parent, false);
    }

    public void returnTag(TextView tag) {
        tagPool.add(tag);
    }

    @CheckResult public View getSpacer(@NonNull ViewGroup parent) {
        if (spacerPool.size() > 0) {
            return spacerPool.remove(0);
        }
        return createSpacer(parent);
    }

    private View createSpacer(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.spacer, parent, false);
    }

    public void returnSpacer(View spacer) {
        spacerPool.add(spacer);
    }

    @CheckResult public ImageView getLivestream(@NonNull ViewGroup parent) {
        if (livestreamPool.size() > 0) {
            return livestreamPool.remove(0);
        }
        return createLivestream(parent);
    }

    private ImageView createLivestream(ViewGroup parent) {
        return (ImageView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.include_schedule_live, parent, false);
    }

    public void returnLivestream(ImageView livestream) {
        livestreamPool.add(livestream);
    }

}
