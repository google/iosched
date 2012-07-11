#!/bin/sh
if [[ -z $ADB ]]; then ADB=adb; fi

$ADB shell am start \
    -a android.intent.action.MAIN \
    -c android.intent.category.LAUNCHER \
    -n com.google.android.apps.iosched/.ui.HomeActivity
