#!/bin/sh
# Remember VERBOSE only works on debug builds of the app
adb shell setprop log.tag.iosched_SyncHelper VERBOSE
adb shell setprop log.tag.iosched_SessionsHandler VERBOSE
adb shell setprop log.tag.iosched_ImageCache VERBOSE
adb shell setprop log.tag.iosched_ImageWorker VERBOSE
adb shell setprop log.tag.iosched_ImageFetcher VERBOSE