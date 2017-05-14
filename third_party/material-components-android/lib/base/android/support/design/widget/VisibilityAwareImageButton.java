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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

class VisibilityAwareImageButton extends ImageButton {

  private int mUserSetVisibility;

  public VisibilityAwareImageButton(Context context) {
    this(context, null);
  }

  public VisibilityAwareImageButton(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public VisibilityAwareImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mUserSetVisibility = getVisibility();
  }

  @Override
  public void setVisibility(int visibility) {
    internalSetVisibility(visibility, true);
  }

  final void internalSetVisibility(int visibility, boolean fromUser) {
    super.setVisibility(visibility);
    if (fromUser) {
      mUserSetVisibility = visibility;
    }
  }

  final int getUserSetVisibility() {
    return mUserSetVisibility;
  }
}
