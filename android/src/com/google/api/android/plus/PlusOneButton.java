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

package com.google.api.android.plus;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Stub until the release of <a href="https://developers.google.com/android/google-play-services/">
 * Google Play Services.</a>
 */
public final class PlusOneButton extends Button {
    public PlusOneButton(Context context) {
        super(context);
    }

    public PlusOneButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlusOneButton(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setUrl(String url) {
    }

    public void setSize(Size size) {
    }

    public enum Size {
        SMALL, MEDIUM, TALL, STANDARD
    }
}
