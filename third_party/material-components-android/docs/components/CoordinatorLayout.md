<!--docs:
title: "Coordinated Behaviors"
layout: detail
section: components
excerpt: "A layout that coordinates interactive behaviors between its children."
path: /catalog/coordinator-layout/
-->

# Coordinated Behaviors

`CoordinatorLayout` is a general-purpose container that allows for
**coordinating interactive behaviors** between its children. It can be used for
implementing single view behaviors, such as drawers that slide in from
off-screen and swipe-dismissable elements, as well as interactions between
multiple views, like moving views out of the way as another dependent view
transitions onto the screen.

## Design & API Documentation

- [Class
  definition](https://github.com/material-components/material-components-android/tree/master/lib/src/android/support/design/widget/CoordinatorLayout.java)
  <!--{: .icon-list-item.icon-list-item--link }-->
- [Class
  overview](https://developer.android.com/reference/android/support/design/widget/CoordinatorLayout.html)
  <!--{: .icon-list-item.icon-list-item--link }-->
<!--{: .icon-list }-->

## Usage

`CoordinatorLayout` manages interactions between its children, and as such needs
to contain all the `View`s that interact with each other. The two general cases
supported by `CoordinatorLayout` are:

- As a top-level content layout (meaning `CoordinatorLayout` is at the root of all
  views within an activity or fragment).
- As a container for a specific interaction with one or more child views.

`CoordinatorLayout` provides a built-in mechanism for anchoring one view to
another. `CoordinatorLayout` also allows children to specify that they inset
certain edges of the screen, and can automatically offset the positions of
children that are anchored to those particular screen edges. For behaviors that
don't fit either of those cases, `CoordinatorLayout` also allows for creating
custom `Behavior` classes.

### Anchors

The `app:layout_anchor` attribute can be set on children of the
`CoordinatorLayout` to attach them to another view. `app:layout_anchorGravity` can
be used to specify where to anchor the child on the other view. A good example
of this is a [floating action button](FloatingActionButton.md) that is anchored
to the bottom-right edge of an [app bar](AppBarLayout.md):

```xml
<android.support.design.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/coordinator_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

  <android.support.design.widget.AppBarLayout
      android:id="@+id/app_bar"
      android:layout_width="match_parent"
      android:layout_height="wrap_content">

    <android.support.v7.widget.Toolbar
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"/>

  </android.support.design.widget.AppBarLayout>

  <!-- Main content -->

  <android.support.design.widget.FloatingActionButton
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:layout_margin="16dp"
      android:contentDescription="@string/add_item"
      android:src="@drawable/ic_add_24dp"
      app:layout_anchor="@id/app_bar"
      app:layout_anchorGravity="bottom|right|end"/>

</android.support.design.widget.CoordinatorLayout>
```

### Insets

`CoordinatorLayout` allows children to specify that they inset certain edges of
the screen, meaning that the child consumes the area of the screen it occupies
and other children should not be placed in that area. Views that inset an edge
of the screen must set the `app:layout_insetEdge` attribute to specify which
edge they inset. Views that should be offset by insets must use
`app:layout_dodgeInsets` to declare which edges they should be affected by. For
instance, any views that include `bottom` in their `app:layout_dodgeInsets` will
be offset to account for the space taken up by views that specify
`app:layout_insetEdge` to be `bottom`.

A good example of this behavior is the default [floating action
button](FloatingActionButton.md) interaction with [snackbars](Snackbar.md). The
floating action button specifies that it needs to dodge `bottom` insets, and
snackbars specify that they inset the `bottom` edge, so floating
action buttons move up when a snackbar becomes visible, and down when
the snackbar is dismissed.

To get the same sort of interaction with a custom view, the
`app:layout_dodgeInsets` attribute can be defined in your layout:

```xml
<android.support.design.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/coordinator_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

  <!-- Main content -->

  <!--
    This element moves up when a snackbar becomes visible, and down when
    the snackbar is dismissed.
  -->
  <LinearLayout
      android:id="@+id/bottom_bar"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:layout_gravity="bottom"
      android:orientation="horizontal"
      app:layout_dodgeInsets="bottom">
    <!-- bottom bar contents -->
  </LinearLayout>
</android.support.design.widget.CoordinatorLayout>
```

### Custom interactions

Custom interactions are defined by associating
[Behaviors](https://developer.android.com/reference/android/support/design/widget/CoordinatorLayout.Behavior.html)
with individual views. This can be done in XML via the `app:layout_behavior`
attribute, and in Java via
[`CoordinatorLayout.LayoutParams`](https://developer.android.com/reference/android/support/design/widget/CoordinatorLayout.LayoutParams.html).
`CoordinatorLayout` also supports specifying a default `Behavior` class
to be used for a particular view type through the
[`@DefaultBehavior`](https://developer.android.com/reference/android/support/design/widget/CoordinatorLayout.DefaultBehavior.html)
annotation.

A number of Material Components classes provide default behaviors that implement
Material Design guidelines, such as:

- [AppBarLayout](AppBarLayout.md), which offsets scrolling views to account for
  the app bar's vertical size, and handles nested scrolling within the content
  views.
- [FloatingActionButton](FloatingActionButton.md), which moves out of the way of
  incoming [Snackbars](Snackbar.md), and can automatically hide when attached to
  a [collapsed app bar](CollapsingToolbarLayout.md), or overlapped by an
  incoming [bottom sheet](BottomSheetBehavior.md).

## Related Concepts

`CoordinatorLayout` and custom Behaviors can be used to build a number of Material
Design patterns, such as:

- [Gestures](https://material.io/guidelines/patterns/gestures.html)
- [Scrolling
  techniques](https://material.io/guidelines/patterns/scrolling-techniques.html)

`CoordinatorLayout` Behaviors also power a number of Material Components,
including:

- [AppBarLayout](AppBarLayout.md)
- [BottomSheetDialogFragment](BottomSheetDialogFragment.md)
- [CollapsingToolbarLayout](CollapsingToolbarLayout.md)
- [FloatingActionButton](FloatingActionButton.md)
- [Snackbar](Snackbar.md)
