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

package android.support.design.internal;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.RestrictTo;
import android.support.design.R;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.MenuView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;

/** @hide */
@RestrictTo(LIBRARY_GROUP)
public class NavigationMenuItemView extends ForegroundLinearLayout implements MenuView.ItemView {

  private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};

  private final int mIconSize;

  private boolean mNeedsEmptyIcon;

  boolean mCheckable;

  private final CheckedTextView mTextView;

  private FrameLayout mActionArea;

  private MenuItemImpl mItemData;

  private ColorStateList mIconTintList;

  private boolean mHasIconTintList;

  private Drawable mEmptyDrawable;

  private final AccessibilityDelegateCompat mAccessibilityDelegate =
      new AccessibilityDelegateCompat() {

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
          super.onInitializeAccessibilityNodeInfo(host, info);
          info.setCheckable(mCheckable);
        }
      };

  public NavigationMenuItemView(Context context) {
    this(context, null);
  }

  public NavigationMenuItemView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public NavigationMenuItemView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setOrientation(HORIZONTAL);
    LayoutInflater.from(context).inflate(R.layout.design_navigation_menu_item, this, true);
    mIconSize = context.getResources().getDimensionPixelSize(R.dimen.design_navigation_icon_size);
    mTextView = (CheckedTextView) findViewById(R.id.design_menu_item_text);
    mTextView.setDuplicateParentStateEnabled(true);
    ViewCompat.setAccessibilityDelegate(mTextView, mAccessibilityDelegate);
  }

  @Override
  public void initialize(MenuItemImpl itemData, int menuType) {
    mItemData = itemData;

    setVisibility(itemData.isVisible() ? VISIBLE : GONE);

    if (getBackground() == null) {
      ViewCompat.setBackground(this, createDefaultBackground());
    }

    setCheckable(itemData.isCheckable());
    setChecked(itemData.isChecked());
    setEnabled(itemData.isEnabled());
    setTitle(itemData.getTitle());
    setIcon(itemData.getIcon());
    setActionView(itemData.getActionView());
    adjustAppearance();
  }

  private boolean shouldExpandActionArea() {
    return mItemData.getTitle() == null
        && mItemData.getIcon() == null
        && mItemData.getActionView() != null;
  }

  private void adjustAppearance() {
    if (shouldExpandActionArea()) {
      // Expand the actionView area
      mTextView.setVisibility(View.GONE);
      if (mActionArea != null) {
        LayoutParams params = (LayoutParams) mActionArea.getLayoutParams();
        params.width = LayoutParams.MATCH_PARENT;
        mActionArea.setLayoutParams(params);
      }
    } else {
      mTextView.setVisibility(View.VISIBLE);
      if (mActionArea != null) {
        LayoutParams params = (LayoutParams) mActionArea.getLayoutParams();
        params.width = LayoutParams.WRAP_CONTENT;
        mActionArea.setLayoutParams(params);
      }
    }
  }

  public void recycle() {
    if (mActionArea != null) {
      mActionArea.removeAllViews();
    }
    mTextView.setCompoundDrawables(null, null, null, null);
  }

  private void setActionView(View actionView) {
    if (actionView != null) {
      if (mActionArea == null) {
        mActionArea =
            (FrameLayout)
                ((ViewStub) findViewById(R.id.design_menu_item_action_area_stub)).inflate();
      }
      mActionArea.removeAllViews();
      mActionArea.addView(actionView);
    }
  }

  private StateListDrawable createDefaultBackground() {
    TypedValue value = new TypedValue();
    if (getContext()
        .getTheme()
        .resolveAttribute(android.support.v7.appcompat.R.attr.colorControlHighlight, value, true)) {
      StateListDrawable drawable = new StateListDrawable();
      drawable.addState(CHECKED_STATE_SET, new ColorDrawable(value.data));
      drawable.addState(EMPTY_STATE_SET, new ColorDrawable(Color.TRANSPARENT));
      return drawable;
    }
    return null;
  }

  @Override
  public MenuItemImpl getItemData() {
    return mItemData;
  }

  @Override
  public void setTitle(CharSequence title) {
    mTextView.setText(title);
  }

  @Override
  public void setCheckable(boolean checkable) {
    refreshDrawableState();
    if (mCheckable != checkable) {
      mCheckable = checkable;
      mAccessibilityDelegate.sendAccessibilityEvent(
          mTextView, AccessibilityEventCompat.TYPE_WINDOW_CONTENT_CHANGED);
    }
  }

  @Override
  public void setChecked(boolean checked) {
    refreshDrawableState();
    mTextView.setChecked(checked);
  }

  @Override
  public void setShortcut(boolean showShortcut, char shortcutKey) {}

  @Override
  public void setIcon(Drawable icon) {
    if (icon != null) {
      if (mHasIconTintList) {
        Drawable.ConstantState state = icon.getConstantState();
        icon = DrawableCompat.wrap(state == null ? icon : state.newDrawable()).mutate();
        DrawableCompat.setTintList(icon, mIconTintList);
      }
      icon.setBounds(0, 0, mIconSize, mIconSize);
    } else if (mNeedsEmptyIcon) {
      if (mEmptyDrawable == null) {
        mEmptyDrawable =
            ResourcesCompat.getDrawable(
                getResources(), R.drawable.navigation_empty_icon, getContext().getTheme());
        if (mEmptyDrawable != null) {
          mEmptyDrawable.setBounds(0, 0, mIconSize, mIconSize);
        }
      }
      icon = mEmptyDrawable;
    }
    TextViewCompat.setCompoundDrawablesRelative(mTextView, icon, null, null, null);
  }

  @Override
  public boolean prefersCondensedTitle() {
    return false;
  }

  @Override
  public boolean showsIcon() {
    return true;
  }

  @Override
  protected int[] onCreateDrawableState(int extraSpace) {
    final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
    if (mItemData != null && mItemData.isCheckable() && mItemData.isChecked()) {
      mergeDrawableStates(drawableState, CHECKED_STATE_SET);
    }
    return drawableState;
  }

  void setIconTintList(ColorStateList tintList) {
    mIconTintList = tintList;
    mHasIconTintList = mIconTintList != null;
    if (mItemData != null) {
      // Update the icon so that the tint takes effect
      setIcon(mItemData.getIcon());
    }
  }

  public void setTextAppearance(int textAppearance) {
    TextViewCompat.setTextAppearance(mTextView, textAppearance);
  }

  public void setTextColor(ColorStateList colors) {
    mTextView.setTextColor(colors);
  }

  public void setNeedsEmptyIcon(boolean needsEmptyIcon) {
    mNeedsEmptyIcon = needsEmptyIcon;
  }
}
