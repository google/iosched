#!/bin/sh
if [[ -z $ADB ]]; then ADB=adb; fi
./kill_process.sh
$ADB shell rm -r /data/data/com.google.android.apps.iosched/*