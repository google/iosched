<!--docs:
title: "Floating Action Buttons"
layout: detail
section: components
excerpt: "A floating button for the primary action in an application."
iconId: button
path: /catalog/floating-action-button/
-->

# Floating Action Buttons

`FloatingActionButton` displays the primary action in an application. It is
a round icon button that's elevated above other page content. **Floating action
buttons** come in a default and mini size.

Floating action buttons provide quick-access to important or common actions
within an app. They have a variety of uses, including:

-   Performing a common action, such as starting a new email in a mail app.
-   Displaying additional related actions.
-   Update or transforming into other UI elements on the screen.

Floating action buttons adjust their position and visibility in response to
other UI elements on the screen. For example, if a [Snackbar](Snackbar.md)
appears, the floating action button shifts its position to stay fully visible.
Or if a [bottom sheet](BottomSheetBehavior.md) partially covers the floating
action button, it may hide itself.

## Design & API Documentation

-   [Material Design
    guidelines: Floating Action Buttons](https://material.io/guidelines/components/buttons-floating-action-button.html)
    <!--{: .icon-list-item.icon-list-item--spec }-->
-   [Class
    definition](https://github.com/material-components/material-components-android/tree/master/lib/src/android/support/design/widget/FloatingActionButton.java)
    <!--{: .icon-list-item.icon-list-item--link }-->
-   [Class
    overview](https://developer.android.com/reference/android/support/design/widget/FloatingActionButton.html)
    <!--{: .icon-list-item.icon-list-item--link }-->
<!--{: .icon-list }-->

## Usage

The `FloatingActionButton` widget provides a complete implementation of Material
Design's floating action button component. Example code of how to include the
widget in your layout:

```xml
<android.support.design.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

  <!-- Main content -->

  <android.support.design.widget.FloatingActionButton
      android:id="@+id/floating_action_button"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:layout_gravity="bottom|right"
      android:layout_margin="16dp"/>

</android.support.design.widget.CoordinatorLayout>
```

Note: If the `FloatingActionButton` is a descendant of a `CoordinatorLayout`,
you get certain behaviors for free. It will automatically shift so that any
displayed [Snackbars](Snackbar.md) do not cover it, and will automatially hide
when covered by an [AppBarLayout](AppBarLayout.md) or
[BottomSheetBehavior](BottomSheetBehavior.md).

Change the icon in the floating action button with:

-   `android:src` attritube
-   `setImageDrawable` method

Change the size of the widget with:

-   `app:fabSize` attribute
-   `setSize` method

Your theme's `colorAccent` provides the default background color of the widget.
Change the background color with:

-   `app:backgroundTint` attribute
-   `setBackgroundTintList` method

Change the elevation of the widget with:

-   `app:elevation` attribute
-   `setCompatElevation` method

### Handling Clicks

`FloatingActionButton` handles clicks in the same way as all views:

```java
FloatingActionButton floatingActionButton =
    (FloatingActionButton) findViewById(R.id.floating_action_button);

floatingActionButton.setOnClickListener(new OnClickListener() {
    @Override
    public void onClick(View v) {
        // Handle the click.
    }
});
```

### Visibility

Use the `show` and `hide` methods to animate the visibility of a
`FloatingActionButton`. The show animation grows the widget and fades it in,
while the hide animation shrinks the widget and fades it out.

## Related Concepts

-   [CoordinatorLayout](CoordinatorLayout.md)
