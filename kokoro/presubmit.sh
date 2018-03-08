#!/bin/bash

# Fail on any error.
set -e
# Display commands to stderr.
set -x

env | sort
pwd
find .
git/iosched/kokoro/build.sh
