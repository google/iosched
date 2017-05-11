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
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.design.R;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.app.AppCompatDialog;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

/** Base class for {@link android.app.Dialog}s styled as a bottom sheet. */
public class BottomSheetDialog extends AppCompatDialog {

  private BottomSheetBehavior<FrameLayout> mBehavior;

  boolean mCancelable = true;
  private boolean mCanceledOnTouchOutside = true;
  private boolean mCanceledOnTouchOutsideSet;

  public BottomSheetDialog(@NonNull Context context) {
    this(context, 0);
  }

  public BottomSheetDialog(@NonNull Context context, @StyleRes int theme) {
    super(context, getThemeResId(context, theme));
    // We hide the title bar for any style configuration. Otherwise, there will be a gap
    // above the bottom sheet when it is expanded.
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
  }

  protected BottomSheetDialog(
      @NonNull Context context, boolean cancelable, OnCancelListener cancelListener) {
    super(context, cancelable, cancelListener);
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    mCancelable = cancelable;
  }

  @Override
  public void setContentView(@LayoutRes int layoutResId) {
    super.setContentView(wrapInBottomSheet(layoutResId, null, null));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
  }

  @Override
  public void setContentView(View view) {
    super.setContentView(wrapInBottomSheet(0, view, null));
  }

  @Override
  public void setContentView(View view, ViewGroup.LayoutParams params) {
    super.setContentView(wrapInBottomSheet(0, view, params));
  }

  @Override
  public void setCancelable(boolean cancelable) {
    super.setCancelable(cancelable);
    if (mCancelable != cancelable) {
      mCancelable = cancelable;
      if (mBehavior != null) {
        mBehavior.setHideable(cancelable);
      }
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (mBehavior != null) {
      mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }
  }

  @Override
  public void setCanceledOnTouchOutside(boolean cancel) {
    super.setCanceledOnTouchOutside(cancel);
    if (cancel && !mCancelable) {
      mCancelable = true;
    }
    mCanceledOnTouchOutside = cancel;
    mCanceledOnTouchOutsideSet = true;
  }

  private View wrapInBottomSheet(int layoutResId, View view, ViewGroup.LayoutParams params) {
    final CoordinatorLayout coordinator =
        (CoordinatorLayout) View.inflate(getContext(), R.layout.design_bottom_sheet_dialog, null);
    if (layoutResId != 0 && view == null) {
      view = getLayoutInflater().inflate(layoutResId, coordinator, false);
    }
    FrameLayout bottomSheet = (FrameLayout) coordinator.findViewById(R.id.design_bottom_sheet);
    mBehavior = BottomSheetBehavior.from(bottomSheet);
    mBehavior.setBottomSheetCallback(mBottomSheetCallback);
    mBehavior.setHideable(mCancelable);
    if (params == null) {
      bottomSheet.addView(view);
    } else {
      bottomSheet.addView(view, params);
    }
    // We treat the CoordinatorLayout as outside the dialog though it is technically inside
    coordinator
        .findViewById(R.id.touch_outside)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                if (mCancelable && isShowing() && shouldWindowCloseOnTouchOutside()) {
                  cancel();
                }
              }
            });
    // Handle accessibility events
    ViewCompat.setAccessibilityDelegate(
        bottomSheet,
        new AccessibilityDelegateCompat() {
          @Override
          public void onInitializeAccessibilityNodeInfo(
              View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            if (mCancelable) {
              info.addAction(AccessibilityNodeInfoCompat.ACTION_DISMISS);
              info.setDismissable(true);
            } else {
              info.setDismissable(false);
            }
          }

          @Override
          public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if (action == AccessibilityNodeInfoCompat.ACTION_DISMISS && mCancelable) {
              cancel();
              return true;
            }
            return super.performAccessibilityAction(host, action, args);
          }
        });
    bottomSheet.setOnTouchListener(
        new View.OnTouchListener() {
          @Override
          public boolean onTouch(View view, MotionEvent event) {
            // Consume the event and prevent it from falling through
            return true;
          }
        });
    return coordinator;
  }

  boolean shouldWindowCloseOnTouchOutside() {
    if (!mCanceledOnTouchOutsideSet) {
      if (Build.VERSION.SDK_INT < 11) {
        mCanceledOnTouchOutside = true;
      } else {
        TypedArray a =
            getContext()
                .obtainStyledAttributes(new int[] {android.R.attr.windowCloseOnTouchOutside});
        mCanceledOnTouchOutside = a.getBoolean(0, true);
        a.recycle();
      }
      mCanceledOnTouchOutsideSet = true;
    }
    return mCanceledOnTouchOutside;
  }

  private static int getThemeResId(Context context, int themeId) {
    if (themeId == 0) {
      // If the provided theme is 0, then retrieve the dialogTheme from our theme
      TypedValue outValue = new TypedValue();
      if (context.getTheme().resolveAttribute(R.attr.bottomSheetDialogTheme, outValue, true)) {
        themeId = outValue.resourceId;
      } else {
        // bottomSheetDialogTheme is not provided; we default to our light theme
        themeId = R.style.Theme_Design_Light_BottomSheetDialog;
      }
    }
    return themeId;
  }

  private BottomSheetBehavior.BottomSheetCallback mBottomSheetCallback =
      new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(
            @NonNull View bottomSheet, @BottomSheetBehavior.State int newState) {
          if (newState == BottomSheetBehavior.STATE_HIDDEN) {
            cancel();
          }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
      };
}
