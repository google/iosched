/*
 * Copyright 2011 Google Inc.
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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * A custom ScrollView that can notify a scroll listener when scrolled.
 */
public class ObservableScrollView extends ScrollView {
    private OnScrollListener mScrollListener;

    public ObservableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mScrollListener != null) {
            mScrollListener.onScrollChanged(this);
        }
    }

    public boolean isScrollPossible() {
        return computeVerticalScrollRange() > getHeight();
    }

    public void setOnScrollListener(OnScrollListener listener) {
        mScrollListener = listener;
    }

    public static interface OnScrollListener {
        public void onScrollChanged(ObservableScrollView view);
    }
}
