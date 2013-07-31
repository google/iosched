#!/bin/sh
# Remember VERBOSE only works on debug builds of the app
adb shell setprop log.tag.iosched_SyncHelper VERBOSE
adb shell setprop log.tag.iosched_SessionsHandler VERBOSE
adb shell setprop log.tag.iosched_BitmapCache VERBOSE
