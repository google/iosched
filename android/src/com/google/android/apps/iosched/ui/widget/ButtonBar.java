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

package com.google.android.apps.iosched.ui.widget;

import com.google.android.apps.iosched.util.UIUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/**
 * An extremely simple {@link LinearLayout} descendant that simply switches the order of its child
 * views on Android 4.0+. The reason for this is that on Android, negative buttons should be shown
 * to the left of positive buttons.
 */
public class ButtonBar extends LinearLayout {

    public ButtonBar(Context context) {
        super(context);
    }

    public ButtonBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public View getChildAt(int index) {
        if (UIUtils.hasICS()) {
            // Flip the buttons so that we get e.g. "Cancel | OK" on ICS
            return super.getChildAt(getChildCount() - 1 - index);
        }
        return super.getChildAt(index);
    }
}
