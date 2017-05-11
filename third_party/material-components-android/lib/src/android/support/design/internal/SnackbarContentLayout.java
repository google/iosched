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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.RestrictTo;
import android.support.design.R;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** @hide */
@RestrictTo(LIBRARY_GROUP)
public class SnackbarContentLayout extends LinearLayout
    implements BaseTransientBottomBar.ContentViewCallback {
  private TextView mMessageView;
  private Button mActionView;

  private int mMaxWidth;
  private int mMaxInlineActionWidth;

  public SnackbarContentLayout(Context context) {
    this(context, null);
  }

  public SnackbarContentLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SnackbarLayout);
    mMaxWidth = a.getDimensionPixelSize(R.styleable.SnackbarLayout_android_maxWidth, -1);
    mMaxInlineActionWidth =
        a.getDimensionPixelSize(R.styleable.SnackbarLayout_maxActionInlineWidth, -1);
    a.recycle();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mMessageView = (TextView) findViewById(R.id.snackbar_text);
    mActionView = (Button) findViewById(R.id.snackbar_action);
  }

  public TextView getMessageView() {
    return mMessageView;
  }

  public Button getActionView() {
    return mActionView;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    if (mMaxWidth > 0 && getMeasuredWidth() > mMaxWidth) {
      widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, MeasureSpec.EXACTLY);
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    final int multiLineVPadding =
        getResources().getDimensionPixelSize(R.dimen.design_snackbar_padding_vertical_2lines);
    final int singleLineVPadding =
        getResources().getDimensionPixelSize(R.dimen.design_snackbar_padding_vertical);
    final boolean isMultiLine = mMessageView.getLayout().getLineCount() > 1;

    boolean remeasure = false;
    if (isMultiLine
        && mMaxInlineActionWidth > 0
        && mActionView.getMeasuredWidth() > mMaxInlineActionWidth) {
      if (updateViewsWithinLayout(
          VERTICAL, multiLineVPadding, multiLineVPadding - singleLineVPadding)) {
        remeasure = true;
      }
    } else {
      final int messagePadding = isMultiLine ? multiLineVPadding : singleLineVPadding;
      if (updateViewsWithinLayout(HORIZONTAL, messagePadding, messagePadding)) {
        remeasure = true;
      }
    }

    if (remeasure) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
  }

  private boolean updateViewsWithinLayout(
      final int orientation, final int messagePadTop, final int messagePadBottom) {
    boolean changed = false;
    if (orientation != getOrientation()) {
      setOrientation(orientation);
      changed = true;
    }
    if (mMessageView.getPaddingTop() != messagePadTop
        || mMessageView.getPaddingBottom() != messagePadBottom) {
      updateTopBottomPadding(mMessageView, messagePadTop, messagePadBottom);
      changed = true;
    }
    return changed;
  }

  private static void updateTopBottomPadding(View view, int topPadding, int bottomPadding) {
    if (ViewCompat.isPaddingRelative(view)) {
      ViewCompat.setPaddingRelative(
          view,
          ViewCompat.getPaddingStart(view),
          topPadding,
          ViewCompat.getPaddingEnd(view),
          bottomPadding);
    } else {
      view.setPadding(view.getPaddingLeft(), topPadding, view.getPaddingRight(), bottomPadding);
    }
  }

  @Override
  public void animateContentIn(int delay, int duration) {
    mMessageView.setAlpha(0f);
    mMessageView.animate().alpha(1f).setDuration(duration).setStartDelay(delay).start();

    if (mActionView.getVisibility() == VISIBLE) {
      mActionView.setAlpha(0f);
      mActionView.animate().alpha(1f).setDuration(duration).setStartDelay(delay).start();
    }
  }

  @Override
  public void animateContentOut(int delay, int duration) {
    mMessageView.setAlpha(1f);
    mMessageView.animate().alpha(0f).setDuration(duration).setStartDelay(delay).start();

    if (mActionView.getVisibility() == VISIBLE) {
      mActionView.setAlpha(1f);
      mActionView.animate().alpha(0f).setDuration(duration).setStartDelay(delay).start();
    }
  }
}
