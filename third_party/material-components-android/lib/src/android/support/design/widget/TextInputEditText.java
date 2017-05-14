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

package android.support.design.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

/**
 * A special sub-class of {@link android.widget.EditText} designed for use as a child of {@link
 * TextInputLayout}.
 *
 * <p>Using this class allows us to display a hint in the IME when in 'extract' mode.
 */
public class TextInputEditText extends AppCompatEditText {

  public TextInputEditText(Context context) {
    super(context);
  }

  public TextInputEditText(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public TextInputEditText(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    final InputConnection ic = super.onCreateInputConnection(outAttrs);
    if (ic != null && outAttrs.hintText == null) {
      // If we don't have a hint and our parent is a TextInputLayout, use it's hint for the
      // EditorInfo. This allows us to display a hint in 'extract mode'.
      ViewParent parent = getParent();
      while (parent instanceof View) {
        if (parent instanceof TextInputLayout) {
          outAttrs.hintText = ((TextInputLayout) parent).getHint();
          break;
        }
        parent = parent.getParent();
      }
    }
    return ic;
  }
}
