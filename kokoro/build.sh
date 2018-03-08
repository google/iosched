#!/bin/bash

# Fail on any error.
set -e
# Display commands to stderr.
set -x

GRADLE_FLAGS=()
if [[ -n "$GRADLE_DEBUG" ]]; then
  GRADLE_FLAGS=( --debug --stacktrace )
fi

# Set up local maven repo
cd $KOKORO_GFILE_DIR
unzip repository.zip
cd repository
mdc_repo_location=$(pwd)

cd $KOKORO_ARTIFACTS_DIR/git/iosched

./gradlew "${GRADLE_FLAGS[@]}" -Pmdc_repo_location="$mdc_repo_location" build
