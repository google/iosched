<!--docs:
title: "Tab Layout"
layout: detail
section: components
excerpt: "A horizontal layout to display tabs."
iconId: tabs
path: /catalog/tab-layout/
-->

# Tab Layout

`TabLayout` provides a horizontal layout to display tabs. The layout handles
interactions for a group of tabs including:

- scrolling behavior,
- (swipe) gestures,
- tab selection,
- animations,
- and alignment.

The Android Developers site provides [detailed documentation](https://developer.android.com/reference/android/support/design/widget/TabLayout.html)
on implementing `TabLayout`.

## Design & API Documentation

- [Material Design guidelines: Tabs](https://material.io/guidelines/components/tabs.html)
  <!--{: .icon-list-item.icon-list-item--spec }-->
- [Class definition](https://github.com/material-components/material-components-android/tree/master/lib/src/android/support/design/widget/TabLayout.java)
  <!--{: .icon-list-item.icon-list-item--link }-->
- [Class overview](https://developer.android.com/reference/android/support/design/widget/TabLayout.html)
  <!--{: .icon-list-item.icon-list-item--link }-->
<!--{: .icon-list }-->

## Usage

The Material Design guidelines for individual tabs is done in the tabs
themselves; the `TabLayout` displays groups of these tabs and controls how they
interact with each other.

A `TabLayout` is a single row of tabs and can be fixed or scrollable. For a fixed
layout, the maximum number of tabs is limited by the size and number of the tabs
and the screen size. Fixed tabs have equal width, based on the widest tab text.

A tab layout should be used above the content associated with the respective
tabs and lets the user quickly change between content views. These content views
are often held in a
[ViewPager](https://developer.android.com/reference/android/support/v4/view/ViewPager.html).

Use [setupWithViewPager(ViewPager)](https://developer.android.com/reference/android/support/design/widget/TabLayout.html#setupWithViewPager(android.support.v4.view.ViewPager))
to link a `TabLayout` with a ViewPager. The
individual tabs in the `TabLayout` will be automatically populated with the page
titles from the PagerAdapter.

Alternatively, you can add a `TabLayout` to a ViewPager in XML:

```xml
<android.support.v4.view.ViewPager
    android:layout_width="match_parent"
    android:layout_height="match_parent">

  <android.support.design.widget.TabLayout
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:layout_gravity="top" />

</android.support.v4.view.ViewPager>
```
