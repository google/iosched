#!/bin/sh
adb shell cat /data/data/com.google.android.apps.iosched/shared_prefs/com.google.android.apps.iosched_preferences.xml | xmllint --format -