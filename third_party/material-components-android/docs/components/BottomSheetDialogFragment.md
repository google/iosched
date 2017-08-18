<!--docs:
title: "Modal Bottom Sheets"
layout: detail
section: components
excerpt: "Modal bottom sheets act like a dialog at the bottom of the screen."
iconId: bottom_sheet
path: /catalog/bottom-sheet-dialog-fragment/
-->

# Modal Bottom Sheets

`BottomSheetDialogFragment` is a thin layer on top of the regular support
library Fragment that renders your fragment as a **modal bottom sheet**,
fundamentally acting as a dialog.

Modal bottom sheets render a shadow on the content below them to indicate
that they are modal, essentially a dialog. If the content outside of the dialog
is tapped then the bottom sheet is dismissed. Modal bottom sheets can be dragged
vertically and dismissed by completely sliding them down.

Note: To implement non-modal **Persistent bottom sheets** use
[BottomSheetBehavior](BottomSheetBehavior.md)
in conjunction with a
[CoordinatorLayout](CoordinatorLayout.md).

## Design & API Documentation

-   [Material Design guidelines: Modal Bottom Sheets](https://material.io/guidelines/components/bottom-sheets.html#bottom-sheets-modal-bottom-sheets)
    <!--{: .icon-list-item.icon-list-item--spec }-->
-   [Class definition](https://github.com/material-components/material-components-android/tree/master/lib/src/android/support/design/widget/BottomSheetDialogFragment.java)
    <!--{: .icon-list-item.icon-list-item--link }-->
    <!-- Styles for list items requiring icons instead of standard bullets. -->
-   [Class overview](https://developer.android.com/reference/android/support/design/widget/BottomSheetDialogFragment.html)
    <!--{: .icon-list-item.icon-list-item--link }-->
<!--{: .icon-list }-->

## Usage

1.  Subclass `BottomSheetDialogFragment`
2.  Override
    [`onCreateView`](https://developer.android.com/reference/android/app/Fragment.html#onCreateView\(android.view.LayoutInflater,%20android.view.ViewGroup,%20android.os.Bundle\)).
3.  Use one of the two versions of
    [`show`](https://developer.android.com/reference/android/support/v4/app/DialogFragment.html#show\(android.support.v4.app.FragmentManager,%20java.lang.String\))
    to display the dialog. *Notice `BottomSheetDialogFragment `is a subclass of
    AppCompatFragment, which means you need to use
    `Activity.getSupportFragmentManager()`.*

Note: Don't call `setOnCancelListener` or `setOnDismissListener` on a
`BottomSheetDialogFragment`, instead you can override `onCancel(DialogInterface)`
or `onDismiss(DialogInterface)` if necessary.

## Related Concepts

`BottomSheetDialogFragment`s are a more modern version of
[Dialogs](https://developer.android.com/guide/topics/ui/dialogs.html). They have
a nicer-looking entrance animation and since they are pinned to the bottom they
may feel easier to use on larger devices.

`BottomSheetDialogFragment`s look very similar to the effects of
[BottomSheetBehavior](BottomSheetBehavior.md) but the latter is not modal and
requires a [CoordinatorLayout](CoordinatorLayout.md).
