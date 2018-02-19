#!/bin/bash

# Fail on any error.
set -e
# Display commands to stderr.
set -x

pwd
find .
git/iosched/kokoro/build.sh
