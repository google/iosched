<!--docs:
title: "Text Fields"
layout: detail
section: components
excerpt: "A text field with an animated floating label and other Material Design features."
iconId: text_field
path: /catalog/text-input-layout/
-->

# Text Fields

![Text Fields](assets/text-fields.svg)
<!--{: .article__asset.article__asset--screenshot }-->

`TextInputLayout` provides an implementation for [Material text
fields](https://material.io/guidelines/components/text-fields.html). Used in
conjunction with a
[`TextInputEditText`](https://developer.android.com/reference/android/support/design/widget/TextInputEditText.html),
`TextInputLayout` makes it easy to include Material **text fields** in your
layouts.

## Design & API Documentation

-   [Material Design
    guidelines: Text Fields](https://material.io/guidelines/components/text-fields.html)
    <!--{: .icon-list-item.icon-list-item--spec }-->
-   [Class
    definition](https://github.com/material-components/material-components-android/tree/master/lib/src/android/support/design/widget/TextInputLayout.java)
    <!--{: .icon-list-item.icon-list-item--link }-->
    <!-- Styles for list items requiring icons instead of standard bullets. -->
-   [Class
    overview](https://developer.android.com/reference/android/support/design/widget/TextInputLayout.html)
    <!--{: .icon-list-item.icon-list-item--link }-->
<!--{: .icon-list }-->

## Usage

To create a material text field, add a `TextInputLayout` to your XML layout and
a `TextInputEditText` as a direct child.

```xml
<android.support.design.widget.TextInputLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

  <android.support.design.widget.TextInputEditText
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:hint="@string/hint_text"/>

</android.support.design.widget.TextInputLayout>
```

Note: You can also use an `EditText` for your input text component. However,
using `TextInputEditText` allows `TextInputLayout` greater control over the
visual aspects of the input text - it allows `TextInputLayout` to display hint
in the text field when in "extract mode" (such as landscape mode).

## Common features

`TextInputLayout` provides functionality for a number of Material [text field
features](https://material.io/guidelines/components/text-fields.html#text-fields-layout).
These are some commonly used properties you can update to control the look of
your text field:

Text field element                     | Relevant attributes/methods
:------------------------------------- | :--------------------------
Label (also called a “Floating Label”) | [`android:hint`](https://developer.android.com/reference/android/support/design/widget/TextInputLayout.html#attr_android:hint)
                                       | [`app:hintEnabled`](https://developer.android.com/reference/android/support/design/widget/TextInputLayout.html#attr_android.support.design:hintEnabled)
Error message                          | [`app:errorEnabled`](https://developer.android.com/reference/android/support/design/widget/TextInputLayout.html#attr_android.support.design:errorEnabled)
                                       | [`#setError(CharSequence)`](https://developer.android.com/reference/android/support/design/widget/TextInputLayout.html#setError(java.lang.CharSequence))
Password redaction                     | [`app:passwordToggleEnabled`](https://developer.android.com/reference/android/support/design/widget/TextInputLayout.html#attr_android.support.design:passwordToggleEnabled)
                                       | [`app:passwordToggleDrawable`](https://developer.android.com/reference/android/support/design/widget/TextInputLayout.html#attr_android.support.design:passwordToggleDrawable)
Character counter                      | [`app:counterEnabled`](https://developer.android.com/reference/android/support/design/widget/TextInputLayout.html#attr_android.support.design:counterEnabled)
                                       | [`app:counterMaxLength`](https://developer.android.com/reference/android/support/design/widget/TextInputLayout.html#attr_android.support.design:counterMaxLength)

## Related concepts

*   [TextView](https://developer.android.com/reference/android/widget/TextView.html)
*   [Specifying the Input Type (Android Developers
    Guide)](https://developer.android.com/training/keyboard-input/style.html)
*   [Copy and Paste (Android Developers
    Guide)](https://developer.android.com/guide/topics/text/copy-paste.html)
