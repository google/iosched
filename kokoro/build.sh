#!/bin/bash

# Fail on any error.
set -e
# Display commands to stderr.
set -x

cd git/iosched

./gradlew build
