
/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui.widget;

import android.content.Context;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.widget.Checkable;

public class CheckableFloatingActionButton extends FloatingActionButton implements Checkable {
    protected boolean mChecked;

    public CheckableFloatingActionButton(Context context) {
        super(context);
    }

    public CheckableFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckableFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private static final int[] CHECKED_STATE_SET = {
            android.R.attr.state_checked
    };

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        setChecked(checked, true);
    }

    public void setChecked(boolean checked, boolean allowAnimate) {
        mChecked = checked;
        refreshDrawableState();
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }
}
