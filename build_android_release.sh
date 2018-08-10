#!/usr/bin/env bash

# Copyright 2018 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

export ANDROID_HOME=$DIR/../../../prebuilts/fullsdk/linux

echo "ANDROID_HOME=$ANDROID_HOME"
cd $DIR

# Build
GRADLE_PARAMS=" --stacktrace"
$DIR/gradlew clean assemble ${GRADLE_PARAMS}
BUILD_RESULT=$?

# Debug
[ ! -d $DIST_DIR/debug ] && mkdir $DIST_DIR/debug
cp $DIR/mobile/build/outputs/apk/debug/mobile-debug.apk $DIST_DIR/debug/
cp $DIR/tv/build/outputs/apk/debug/tv-debug.apk $DIST_DIR/debug/
cp $DIR/wear/build/outputs/apk/debug/wear-debug.apk $DIST_DIR/debug/

# Staging
[ ! -d $DIST_DIR/staging ] && mkdir $DIST_DIR/staging
cp $DIR/mobile/build/outputs/apk/staging/mobile-staging.apk $DIST_DIR/staging/
cp $DIR/tv/build/outputs/apk/staging/tv-staging.apk $DIST_DIR/staging/

# Release
[ ! -d $DIST_DIR/release ] && mkdir $DIST_DIR/release
cp $DIR/mobile/build/outputs/apk/release/mobile-release-unsigned.apk $DIST_DIR/release/mobile-release.apk
cp $DIR/tv/build/outputs/apk/release/tv-release-unsigned.apk $DIST_DIR/release/tv-release.apk
cp $DIR/wear/build/outputs/apk/release/wear-release-unsigned.apk $DIST_DIR/release/wear-release.apk
cp $DIR/mobile/build/outputs/mapping/release/mapping.txt $DIST_DIR/release/mobile-mapping.txt
cp $DIR/tv/build/outputs/mapping/release/mapping.txt $DIST_DIR/release/tv-mapping.txt

exit $BUILD_RESULT
