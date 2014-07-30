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
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.MyScheduleAdapter;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * My Schedule view. This view is a linear layout showing all schedule items from an adapter.
 * This is different from the MyScheduleFragment, which is a ListFragment based on the adapter.
 * The fundamental difference is that while the ListFragment has built-in scrolling, this
 * view does NOT scroll, it resizes to fit all items. It is suitable for use as part of a
 * larger view where you want the larger view to scroll as one, with this list inside it.
 */
public class MyScheduleView extends LinearLayout {
    private static final String TAG = makeLogTag("MyScheduleView");

    MyScheduleAdapter mAdapter = null;
    DataSetObserver mObserver = null;

    public MyScheduleView(Context context) {
        this(context, null, 0);
    }

    public MyScheduleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyScheduleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOrientation(LinearLayout.VERTICAL);
    }

    public void setAdapter(MyScheduleAdapter adapter) {
        if (mAdapter != null && mObserver != null) {
            mAdapter.unregisterDataSetObserver(mObserver);
            mObserver = null;
        }
        mAdapter = adapter;
        rebuild();
        if (mAdapter != null) {
            mAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    rebuild();
                }

                @Override
                public void onInvalidated() {
                    setAdapter(null);
                }
            });
        }
    }

    private void setViewAt(int i, View view) {
        if (i < getChildCount()) {
            View viewToReplace = getChildAt(i);
            if (viewToReplace != view) {
                addView(view, i);
                removeView(viewToReplace);
            }
        } else {
            addView(view);
        }
    }

    public void rebuild() {
        LOGD(TAG, "Rebuilding MyScheduleView.");
        int i;
        int count = (mAdapter == null) ? 0 : mAdapter.getCount();
        LOGD(TAG, "Adapter has " + count + " items.");

        for (i = 0; i < count; i++) {
            LOGD(TAG, "Setting up view#" + i);
            View recycle = i < getChildCount() ? getChildAt(i) : null;
            LOGD(TAG, "view#" + i + ", recycle=" + recycle);
            View view = mAdapter.getView(i, recycle, this);
            if (i < getChildCount()) {
                LOGD(TAG, "setting view#" + i);
                setViewAt(i, view);
            } else {
                LOGD(TAG, "adding view #" + i);
                addView(view);
            }
        }
        for (; i < getChildCount(); i++) {
            LOGD(TAG, "removing view #" + i);
            removeViewAt(i);
        }

        requestLayout();
    }
}
