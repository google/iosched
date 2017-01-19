    Copyright 2016 Google Inc. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


# Navigating the Android app

The app is in the *android* folder. Inside the *src* folder, the following folders are available:

+ **main**: the app code. It is packaged by feature, and not by layer. The only classes packaged by 
layers are those common to all features, such as BaseActivity. Packing by feature enables different
developers to easily work on different features in a contained manner. It also enables a new 
developer to understand what features the app has simply by scanning the package list, and to 
understand a given feature by reading through the code in a given package. 
+ **debug**: used for resources for the debug build.
+ **androidTest**: contains instrumentation tests. Those are written with mock data so they are not 
dependent on the backend. They can be run on either a device or an emulator.
+ **test**: contains unit tests. Those are plain Java unit tests and do not need a device or an 
emulator to be run.

# Diving into the main code

The main packages are:

+ **archframework**: classes used for the mvp framework. [Refer to the MVP Framework doc]
(MVP_FRAMEWORK.md).
+ **explore**: the *explore* feature. This is the home screen for the app, and it also accessible
from the navigation drawer.
+ **feedback**: the *session feedback* feature, where the user can provide feedback for a session.
This feature is accessible from various other screens.
+ **gcm**: this handles the notifications received by the backend. [Refer to the GCM doc] (GCM.md).
+ **io**: this handles the conversion of the json from the backend into POJOs. It uses the 
Gson library.
+ **map**: the *map* feature.
+ **myschedule**: the *schedule* feature. This is accessible from the navigation drawer.
+ **provider**: the content provider and contract used for all the data in the app.
+ **session**: the *session details* feature. This is accessible when selecting a session from 
either *explore* or *schedule* features. Sessions that the user views are indexed using the [App 
Indexing API] (https://developers.google.com/app-indexing/introduction#android), and will appear as 
search suggestions in the Google app.
+ **sync**: this contains all the logic to handle the *backend sync* of the data. [Refer to the sync 
doc] (SYNC.md).
+ **ui**: contains custom Activities which are extended and used by different features, as well as 
custom views which are also used by different features.
+ **videolibrary**: the *video library* feature. This is accessible from the navigation drawer.
+ **welcome**: the *welcome* feature. This is shown to the user the first time they launch the app. 
It includes the login screen.

# Diving into the test code

+ **test classes**: packages correspond to packages in the app. Classes that are actual tests are 
named following the *ClassTest* convention where "Class" is the class under test. If several test
classes exist for a given class under test, they are named following the 
*Class_UseCaseDescriptionTest* convention, where "UseCaseDescription" defines the conditions of the
test. An example would be "SessionDetailActivity_KeynoteSessionTest".
+ **test methods**: for unit tests, they are named following the 
*MethodUnderTest_DataType_ExpectedResult* convention. For UI tests, they are named following the 
*ViewUnderTest_ActionOnView_ExpectedResult*. Note that *ActionOnView* may be omitted for some tests.
+ **UI tests**: UI tests are written using the [Android Testing Support Library]
 (https://google.github.io/android-testing-support-library/).
