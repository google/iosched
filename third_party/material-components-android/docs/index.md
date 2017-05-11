# Material Components for Android

## Getting Started

### Using the support library version

For most users, the Android Design support library is the best version to use in
their application. The Android SDK contains the latest stable version of this
library. To use it:

1. Make sure you've downloaded the Android Support Repository using the SDK
   Manager.
2. Open the `build.gradle` file for your application.
3. Add the Design support library to the `dependencies` section:

  ```groovy
  dependencies {
    // ...
    compile 'com.android.support:design:25.3.1'
    // ...
  }
  ```

### Building from source

If you'll be contributing to the library, or need a version newer than what has
been released in the Android support libraries, Material Components for Android
can also be built from source. To do so:

Clone this repository:

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

```
./gradlew test
```

To run the emulator tests, ensure you have [a virtual device set
up](https://developer.android.com/studio/run/managing-avds.html) and do:

```
./gradlew connectedAndroidTest
```


