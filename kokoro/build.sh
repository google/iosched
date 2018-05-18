#!/bin/bash

# Fail on any error.
set -e
# Display commands to stderr.
set -x

GRADLE_FLAGS=()
if [[ -n "$GRADLE_DEBUG" ]]; then
  GRADLE_FLAGS=( --debug --stacktrace )
fi

cd $KOKORO_ARTIFACTS_DIR/git/iosched

./gradlew "${GRADLE_FLAGS[@]}" build
