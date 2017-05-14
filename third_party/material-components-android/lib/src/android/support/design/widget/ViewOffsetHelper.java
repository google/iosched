/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.design.widget;

import android.support.v4.view.ViewCompat;
import android.view.View;

/**
 * Utility helper for moving a {@link android.view.View} around using {@link
 * android.view.View#offsetLeftAndRight(int)} and {@link android.view.View#offsetTopAndBottom(int)}.
 *
 * <p>Also the setting of absolute offsets (similar to translationX/Y), rather than additive
 * offsets.
 */
class ViewOffsetHelper {

  private final View mView;

  private int mLayoutTop;
  private int mLayoutLeft;
  private int mOffsetTop;
  private int mOffsetLeft;

  public ViewOffsetHelper(View view) {
    mView = view;
  }

  public void onViewLayout() {
    // Now grab the intended top
    mLayoutTop = mView.getTop();
    mLayoutLeft = mView.getLeft();

    // And offset it as needed
    updateOffsets();
  }

  private void updateOffsets() {
    ViewCompat.offsetTopAndBottom(mView, mOffsetTop - (mView.getTop() - mLayoutTop));
    ViewCompat.offsetLeftAndRight(mView, mOffsetLeft - (mView.getLeft() - mLayoutLeft));
  }

  /**
   * Set the top and bottom offset for this {@link ViewOffsetHelper}'s view.
   *
   * @param offset the offset in px.
   * @return true if the offset has changed
   */
  public boolean setTopAndBottomOffset(int offset) {
    if (mOffsetTop != offset) {
      mOffsetTop = offset;
      updateOffsets();
      return true;
    }
    return false;
  }

  /**
   * Set the left and right offset for this {@link ViewOffsetHelper}'s view.
   *
   * @param offset the offset in px.
   * @return true if the offset has changed
   */
  public boolean setLeftAndRightOffset(int offset) {
    if (mOffsetLeft != offset) {
      mOffsetLeft = offset;
      updateOffsets();
      return true;
    }
    return false;
  }

  public int getTopAndBottomOffset() {
    return mOffsetTop;
  }

  public int getLeftAndRightOffset() {
    return mOffsetLeft;
  }

  public int getLayoutTop() {
    return mLayoutTop;
  }

  public int getLayoutLeft() {
    return mLayoutLeft;
  }
}
