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
OUTPUTS=$DIR/mobile/build/outputs

export ANDROID_HOME="$(cd $DIR/../../../prebuilts/fullsdk/linux && pwd )"

echo "ANDROID_HOME=$ANDROID_HOME"
cd $DIR

# Build
GRADLE_PARAMS=" --stacktrace"
$DIR/gradlew clean assemble ${GRADLE_PARAMS}
BUILD_RESULT=$?

# Debug
cp $OUTPUTS/apk/debug/mobile-debug.apk $DIST_DIR/mobile_debug.apk

# Staging
cp $OUTPUTS/apk/staging/mobile-staging.apk $DIST_DIR/mobile_staging.apk

# Release
cp $OUTPUTS/apk/release/mobile-release-unsigned.apk $DIST_DIR/mobile_release.apk
cp $OUTPUTS/mapping/release/mapping.txt $DIST_DIR/mobile_release_apk_mapping.txt

# Build App Bundles
# Don't clean here, otherwise all apks are gone.
$DIR/gradlew bundle ${GRADLE_PARAMS}

# Debug
cp $OUTPUTS/bundle/debug/mobile-debug.aab $DIST_DIR/mobile_debug.aab

# Staging
cp $OUTPUTS/bundle/staging/mobile-staging.aab $DIST_DIR/mobile_staging.aab

# Release
cp $OUTPUTS/bundle/release/mobile-release.aab $DIST_DIR/mobile_release.aab
cp $OUTPUTS/mapping/release/mapping.txt $DIST_DIR/mobile_relase_aab_mapping.txt

BUILD_RESULT=$?

exit $BUILD_RESULT
