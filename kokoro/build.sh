#!/bin/bash

# Fail on any error.
set -e
# Display commands to stderr.
set -x

# Set up local maven repo
cd $KOKORO_GFILE_DIR
unzip repository.zip
cd repository
mdc_repo_location=$(pwd)

cd $KOKORO_ARTIFACTS_DIR/git/iosched

./gradlew -Pmdc_repo_location="$mdc_repo_location" build
