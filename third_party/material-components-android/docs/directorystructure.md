# Material component directory structure

All of the Material Components are located under **[lib/](https://github.com/material-components/material-components-android/tree/master/lib)**. The library is
sub-divided into:

*   [base/](components.md#base/)
*   platform-specific overrides
*   [src/](components.md#src/))

Any widgets that require a platform-specific implementation can be found in
the **base/** directory, while all other widgets are located under **src/**.

Classes in the library are separated into two directories:

*   [internal/](https://github.com/material-components/material-components-android/tree/master/lib/src/android/support/design/internal/)
*   [widget/](https://github.com/material-components/material-components-android/tree/master/lib/src/android/support/design/widget/)

Classes in **widget/** comprise the public API; these can be used directly in your
applications. Classes in **internal/** are part of the protected API and are used to
support the public API classes.
