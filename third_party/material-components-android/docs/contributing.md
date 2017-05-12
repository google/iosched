<!--docs:
title: "Contributing"
layout: landing
section: docs
path: /docs/contributing/
-->

# General Contributing Guidelines

The Material Components contributing policies and procedures can be found in the
main Material Components documentation repository’s
[contributing page](https://github.com/material-components/material-components/blob/develop/CONTRIBUTING.md).

## Android Additions

The Android team also abides by the following policy items.

### Code conventions

Since we all want to spend more time coding and less time fiddling with
whitespace, Material Components for Android uses code conventions and styles to
encourage consistency. Code with a consistent style is easier (and less
error-prone!) to review, maintain, and understand.

##### Be consistent

If the style guide is not explicit about a particular situation, the cardinal
rule is to **be consistent**. For example, take a look at the surrounding code
and follow its lead, or look for similar cases elsewhere in the codebase.

##### Java

We follow the
[Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).

##### XML

- 2 space indentation
- Resource naming (including IDs) is `lowercase_with_underscores`
- Attribute ordering:
  1. `xmlns:android`
  2. other `xmlns:`
  3. `android:id`
  4. `style`
  5. `android:layout_` attributes
  6. `android:padding` attributes
  7. other `android:` attributes
  8. `app:` attributes
  9. `tool:` attributes
