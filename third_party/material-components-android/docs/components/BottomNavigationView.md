<!--docs:
title: "Bottom Navigation"
layout: detail
section: components
excerpt: "Bottom navigation bars make it easy to explore and switch between top-level views in a single tap."
iconId: bottom_navigation
path: /catalog/bottom-navigation-view/
-->

# Bottom Navigation

![Bottom Navigation](assets/bottom-navigation.svg)
<!--{: .article__asset.article__asset--screenshot }-->

`BottomNavigationView` creates **bottom navigation** bars, making it easy to
explore and switch between top-level content views with a single tap.

This pattern can be used when you have between 3 and 5 top-level destinations to
navigate to.

## Design & API Documentation

-   [Material Design guidelines: Bottom Navigation](https://material.io/guidelines/components/bottom-navigation.html)
    <!--{: .icon-list-item.icon-list-item--spec }-->
-   [Class definition](https://github.com/material-components/material-components-android/tree/master/lib/src/android/support/design/widget/BottomNavigationView.java)
    <!--{: .icon-list-item.icon-list-item--link }-->
    <!-- Styles for list items requiring icons instead of standard bullets. -->
-   [Class overview](https://developer.android.com/reference/android/support/design/widget/BottomNavigationView.html)
    <!--{: .icon-list-item.icon-list-item--link }-->
<!--{: .icon-list }-->

## Usage

1. Create a [menu
resource](https://developer.android.com/guide/topics/resources/menu-resource.html)
with up to 5 navigation targets (`BottomNavigationView` does not support more than
5 items).
2. Lay out your `BottomNavigationView` below your content.
3. Set the `app:menu` attribute on your `BottomNavigationView` to your menu
resource.
4. Listen for selection events using `setOnNavigationItemSelectedListener(...)`.

A typical layout file would look like this:

```xml
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

  <!-- Main content -->

  <android.support.design.widget.BottomNavigationView
      android:id="@+id/bottom_navigation"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:layout_gravity="bottom"
      app:itemBackground="@color/colorPrimary"
      app:itemIconTint="@color/white"
      app:itemTextColor="@color/white"
      app:menu="@menu/navigation_menu" />

</FrameLayout>
```

### Handling Enabled/States

The `app:itemIconTint` and `app:itemTextColor` take a
[ColorStateList](https://developer.android.com/reference/android/content/res/ColorStateList.html)
instead of a simple color, this means that you can write a selector for these
colors that accounts for enabled/disabled status.

For example, you could have a `navigation_colors.xml` that contains:

```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
  <item
      android:state_enabled="true"
      android:color="@color/white" />
  <item
      android:state_enabled="false"
      android:color="@color/colorPrimaryDark" />
 </selector>
```

And you would use it like this on your `BottomNavigationView`:

```xml
<android.support.design.widget.BottomNavigationView
    android:id="@+id/bottom_navigation"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_alignParentBottom="true"
    app:itemBackground="@color/colorPrimary"
    app:itemIconTint="@drawable/navigation_colors"
    app:itemTextColor="@drawable/navigation_colors"
    app:menu="@menu/navigation_menu" />
```

## Related Concepts

There are other navigation patterns you should be aware of

-   [Hierarchical navigation](https://developer.android.com/training/implementing-navigation/index.html).
    *See also [Navigation with Back and
    Up](https://developer.android.com/design/patterns/navigation.html).*
-   Swipeable tabs using [TabLayout](TabLayout.md) and
    [ViewPager](https://developer.android.com/reference/android/support/v4/view/ViewPager.html).
-   Using [NavigationView](NavigationView.md) to display a longer list of
    navigation targets, usually in a navigation drawer.
