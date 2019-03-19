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
MOBILE_OUT=$DIR/mobile/build/outputs
TV_OUT=$DIR/tv/build/outputs

export ANDROID_HOME="$(cd $DIR/../../../prebuilts/fullsdk/linux && pwd )"

echo "ANDROID_HOME=$ANDROID_HOME"
cd $DIR

# Build
GRADLE_PARAMS=" --stacktrace"
$DIR/gradlew clean assemble ${GRADLE_PARAMS}
BUILD_RESULT=$?

# Debug
[ ! -d $DIST_DIR/debug ] && mkdir $DIST_DIR/debug
cp $MOBILE_OUT/apk/debug/mobile-debug.apk $DIST_DIR/debug/
cp $TV_OUT/apk/debug/tv-debug.apk $DIST_DIR/debug/

# Staging
[ ! -d $DIST_DIR/staging ] && mkdir $DIST_DIR/staging
cp $MOBILE_OUT/apk/staging/mobile-staging.apk $DIST_DIR/staging/
cp $TV_OUT/apk/staging/tv-staging.apk $DIST_DIR/staging/

# Release
[ ! -d $DIST_DIR/release ] && mkdir $DIST_DIR/release
cp $MOBILE_OUT/apk/release/mobile-release-unsigned.apk $DIST_DIR/release/mobile-release.apk
cp $MOBILE_OUT/mapping/release/mapping.txt $DIST_DIR/release/mobile-release-apk-mapping.txt
cp $TV_OUT/apk/release/tv-release-unsigned.apk $DIST_DIR/release/tv-release.apk
cp $TV_OUT/mapping/release/mapping.txt $DIST_DIR/release/tv-release-apk-mapping.txt

# Build App Bundles
# Don't clean here, otherwise all apks are gone.
$DIR/gradlew bundle ${GRADLE_PARAMS}

# Debug
cp $MOBILE_OUT/bundle/debug/mobile.aab $DIST_DIR/debug/mobile-debug.aab
cp $TV_OUT/bundle/debug/tv.aab $DIST_DIR/debug/tv-debug.aab

# Staging
cp $MOBILE_OUT/bundle/staging/mobile.aab $DIST_DIR/staging/mobile-staging.aab
cp $TV_OUT/bundle/staging/tv.aab $DIST_DIR/staging/tv-staging.aab

# Release
cp $MOBILE_OUT/bundle/release/mobile.aab $DIST_DIR/release/mobile-release.aab
cp $MOBILE_OUT/mapping/release/mapping.txt $DIST_DIR/release/mobile-release-aab-mapping.txt
cp $TV_OUT/bundle/release/tv.aab $DIST_DIR/release/tv-release.aab
cp $TV_OUT/mapping/release/mapping.txt $DIST_DIR/release/tv-release-aab-mapping.txt
BUILD_RESULT=$?

exit $BUILD_RESULT
