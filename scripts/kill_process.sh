#!/bin/sh
if [[ -z $ADB ]]; then ADB=adb; fi
$ADB shell am force-stop com.google.android.apps.iosched
