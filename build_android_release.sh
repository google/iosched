#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export ANDROID_HOME=$DIR/../../../prebuilts/fullsdk/linux

echo $ANDROID_HOME
cd $DIR

# Build
GRADLE_PARAMS=" --stacktrace --offline"
$DIR/gradlew clean assemble ${GRADLE_PARAMS}
BUILD_RESULT=$?

# Debug
[ ! -d $DIST_DIR/debug ] && mkdir $DIST_DIR/debug
cp $DIR/mobile/build/outputs/apk/debug/mobile-debug.apk $DIST_DIR/debug/
cp $DIR/tv/build/outputs/apk/debug/tv-debug.apk $DIST_DIR/debug/

# Release
[ ! -d $DIST_DIR/release ] && mkdir $DIST_DIR/release
cp $DIR/mobile/build/outputs/apk/release/mobile-release-unsigned.apk $DIST_DIR/release/
cp $DIR/tv/build/outputs/apk/release/tv-release-unsigned.apk $DIST_DIR/release/

exit $BUILD_RESULT
