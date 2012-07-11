#!/bin/sh
if [[ -z $ADB ]]; then ADB=adb; fi
$ADB shell "echo '$*' | sqlite3 -header -column /data/data/com.google.android.apps.iosched/databases/schedule.db"