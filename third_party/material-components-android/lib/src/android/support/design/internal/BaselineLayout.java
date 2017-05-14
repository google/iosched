/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.design.internal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple ViewGroup that aligns all the views inside on a baseline. Note: bottom padding for this
 * view will be measured starting from the baseline.
 *
 * @hide
 */
public class BaselineLayout extends ViewGroup {
  private int mBaseline = -1;

  public BaselineLayout(Context context) {
    super(context, null, 0);
  }

  public BaselineLayout(Context context, AttributeSet attrs) {
    super(context, attrs, 0);
  }

  public BaselineLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int count = getChildCount();
    int maxWidth = 0;
    int maxHeight = 0;
    int maxChildBaseline = -1;
    int maxChildDescent = -1;
    int childState = 0;

    for (int i = 0; i < count; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() == GONE) {
        continue;
      }

      measureChild(child, widthMeasureSpec, heightMeasureSpec);
      final int baseline = child.getBaseline();
      if (baseline != -1) {
        maxChildBaseline = Math.max(maxChildBaseline, baseline);
        maxChildDescent = Math.max(maxChildDescent, child.getMeasuredHeight() - baseline);
      }
      maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
      maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
      childState = View.combineMeasuredStates(childState, child.getMeasuredState());
    }
    if (maxChildBaseline != -1) {
      maxChildDescent = Math.max(maxChildDescent, getPaddingBottom());
      maxHeight = Math.max(maxHeight, maxChildBaseline + maxChildDescent);
      mBaseline = maxChildBaseline;
    }
    maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
    maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
    setMeasuredDimension(
        View.resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
        View.resolveSizeAndState(
            maxHeight, heightMeasureSpec, childState << MEASURED_HEIGHT_STATE_SHIFT));
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    final int count = getChildCount();
    final int parentLeft = getPaddingLeft();
    final int parentRight = right - left - getPaddingRight();
    final int parentContentWidth = parentRight - parentLeft;
    final int parentTop = getPaddingTop();

    for (int i = 0; i < count; i++) {
      final View child = getChildAt(i);
      if (child.getVisibility() == GONE) {
        continue;
      }

      final int width = child.getMeasuredWidth();
      final int height = child.getMeasuredHeight();

      final int childLeft = parentLeft + (parentContentWidth - width) / 2;
      final int childTop;
      if (mBaseline != -1 && child.getBaseline() != -1) {
        childTop = parentTop + mBaseline - child.getBaseline();
      } else {
        childTop = parentTop;
      }

      child.layout(childLeft, childTop, childLeft + width, childTop + height);
    }
  }

  @Override
  public int getBaseline() {
    return mBaseline;
  }
}
