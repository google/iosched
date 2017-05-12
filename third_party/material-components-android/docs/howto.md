<!--docs:
title: "How to use Material Components for Android"
layout: landing
section: docs
path: /docs/
-->

# How to use Material Components for Android

Material Components for Android is available through Android's SDK manager. To
use it:

1. Make sure you've downloaded the Android Support Repository using the SDK
Manager.
2. Open the build.gradle file for your application.
3. Add the library to the dependencies section:

  ```groovy
    dependencies {
      // ...
      compile 'com.android.support:design:[Library version code]'
      // ...
    }
  ```

## Contributors

Material Components for Android welcomes contributions from the community. Check
out our [contributing guidelines](contributing.md) as well as an overview of
the [directory structure](directorystructure.md) before getting started.

To make a contribution, you'll need to be able to build the library from source
and run our tests.

### Building from source

If you'll be contributing to the library, or need a version newer than what has
been released in the Android support libraries, Material Components for Android
can also be built from source. To do so:

Clone the repository:

```sh
git clone https://github.com/material-components/material-components-android.git
```

Then, build an AAR using Gradle:

```sh
./gradlew assembleRelease
```

(the AAR file will be located in `lib/build/outputs/aar/`)

### Running tests

Material Components for Android has JVM tests as well as Emulator tests.

To run the JVM tests, do:

```sh
./gradlew test
```

To run the emulator tests, ensure you have [a virtual device set
up](https://developer.android.com/studio/run/managing-avds.html) and do:

```sh
./gradlew connectedAndroidTest
```


## Useful Links
- [Contributing](contributing.md)
- [Class
  documentation](https://developer.android.com/reference/android/support/design/widget/package-summary.html)
- [MDC-Android on Stack
  Overflow](https://www.stackoverflow.com/questions/tagged/material-components+android)
- [Android Developer’s
  Guide](https://developer.android.com/training/material/index.html)
- [Material.io](https://www.material.io)
- [Material Design Guidelines](https://material.google.com)
