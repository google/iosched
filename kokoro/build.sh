#!/bin/bash

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

# Fail on any error.
set -e
# Display commands to stderr.
set -x

GRADLE_FLAGS=()
if [[ -n "$GRADLE_DEBUG" ]]; then
  GRADLE_FLAGS=( --debug --stacktrace )
fi

export ANDROID_SDK_HOME=/opt/android-sdk/current
echo y | ${ANDROID_SDK_HOME}/tools/bin/sdkmanager "build-tools;28.0.3"

cd $KOKORO_ARTIFACTS_DIR/git/iosched

./gradlew "${GRADLE_FLAGS[@]}" build


# For Firebase Test Lab
SERVICE_ACCOUNT_KEY=${KOKORO_GFILE_DIR}/adssched-f8469c478ecc.json
gcloud config set project adssched
gcloud auth activate-service-account firebasetestlabforkokoro@adssched.iam.gserviceaccount.com --key-file ${SERVICE_ACCOUNT_KEY}

./gradlew mobile:assembleAndroidTest
./gradlew mobile:assembleStaging

MAX_RETRY=3
run_firebase_test_lab() {
  ## Retry can be done by passing the --num-flaky-test-attempts to gcloud, but gcloud SDK in the
  ## kokoro server doesn't support it yet.

  set +e # To not exit on an error to retry flaky tests
  local counter=0
  local result=1
  while [ $result != 0 -a $counter -lt $MAX_RETRY ]; do
    gcloud firebase test android run \
        --type instrumentation \
        --app  mobile/build/outputs/apk/staging/mobile-staging.apk \
        --test mobile/build/outputs/apk/androidTest/staging/mobile-staging-androidTest.apk \
        --device-ids hammerhead,walleye,blueline \
        --os-version-ids 21,26,28,Q-beta-3 \
        --locales en \
        --timeout 60
    result=$?
    let counter=counter+1
  done
  return $result
}

run_firebase_test_lab
exit $?
