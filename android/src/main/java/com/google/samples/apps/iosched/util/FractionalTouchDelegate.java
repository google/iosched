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

package com.google.samples.apps.iosched.util;

import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;

/**
 * {@link TouchDelegate} that gates {@link MotionEvent} instances by comparing
 * then against fractional dimensions of the source view.
 * <p>
 * This is particularly useful when you want to define a rectangle in terms of
 * the source dimensions, but when those dimensions might change due to pending
 * or future layout passes.
 * <p>
 * One example is catching touches that occur in the top-right quadrant of
 * {@code sourceParent}, and relaying them to {@code targetChild}. This could be
 * done with: <code>
 * FractionalTouchDelegate.setupDelegate(sourceParent, targetChild, new RectF(0.5f, 0f, 1f, 0.5f));
 * </code>
 */
public class FractionalTouchDelegate extends TouchDelegate {

    private View mSource;
    private View mTarget;

    private RectF mSourceFraction;

    private Rect mScrap = new Rect();

    /** Cached full dimensions of {@link #mSource}. */
    private Rect mSourceFull = new Rect();
    /** Cached projection of {@link #mSourceFraction} onto {@link #mSource}. */
    private Rect mSourcePartial = new Rect();

    private boolean mDelegateTargeted;

    public FractionalTouchDelegate(View source, View target, RectF sourceFraction) {
        super(new Rect(0, 0, 0, 0), target);
        mSource = source;
        mTarget = target;
        mSourceFraction = sourceFraction;
    }

    /**
     * Helper to create and setup a {@link FractionalTouchDelegate} between the
     * given {@link View}.
     *
     * @param source Larger source {@link View}, usually a parent, that will be
     *            assigned {@link View#setTouchDelegate(TouchDelegate)}.
     * @param target Smaller target {@link View} which will receive
     *            {@link MotionEvent} that land in requested fractional area.
     * @param sourceFraction Fractional area projected onto source {@link View}
     *            which determines when {@link MotionEvent} will be passed to
     *            target {@link View}.
     */
    public static void setupDelegate(View source, View target, RectF sourceFraction) {
        source.setTouchDelegate(new FractionalTouchDelegate(source, target, sourceFraction));
    }

    /**
     * Consider updating {@link #mSourcePartial} when {@link #mSource}
     * dimensions have changed.
     */
    private void updateSourcePartial() {
        mSource.getHitRect(mScrap);
        if (!mScrap.equals(mSourceFull)) {
            // Copy over and calculate fractional rectangle
            mSourceFull.set(mScrap);

            final int width = mSourceFull.width();
            final int height = mSourceFull.height();

            mSourcePartial.left = (int) (mSourceFraction.left * width);
            mSourcePartial.top = (int) (mSourceFraction.top * height);
            mSourcePartial.right = (int) (mSourceFraction.right * width);
            mSourcePartial.bottom = (int) (mSourceFraction.bottom * height);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        updateSourcePartial();

        // The logic below is mostly copied from the parent class, since we
        // can't update private mBounds variable.

        // http://android.git.kernel.org/?p=platform/frameworks/base.git;a=blob;
        // f=core/java/android/view/TouchDelegate.java;hb=eclair#l98

        final Rect sourcePartial = mSourcePartial;
        final View target = mTarget;

        int x = (int)event.getX();
        int y = (int)event.getY();

        boolean sendToDelegate = false;
        boolean hit = true;
        boolean handled = false;

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            if (sourcePartial.contains(x, y)) {
                mDelegateTargeted = true;
                sendToDelegate = true;
            }
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_MOVE:
            sendToDelegate = mDelegateTargeted;
            if (sendToDelegate) {
                if (!sourcePartial.contains(x, y)) {
                    hit = false;
                }
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            sendToDelegate = mDelegateTargeted;
            mDelegateTargeted = false;
            break;
        }

        if (sendToDelegate) {
            if (hit) {
                event.setLocation(target.getWidth() / 2, target.getHeight() / 2);
            } else {
                event.setLocation(-1, -1);
            }
            handled = target.dispatchTouchEvent(event);
        }
        return handled;
    }
}
