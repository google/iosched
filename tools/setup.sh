#!/bin/bash

RED='\033[0;1;31m'
NC='\033[0m' # No Color

GIT_DIR=$(git rev-parse --git-dir 2> /dev/null)
GIT_ROOT=$(git rev-parse --show-toplevel 2> /dev/null)

if [[ ! "$GIT_ROOT" =~ /iosched$ ]]; then
  echo -e "${RED}ERROR:${NC} Please run this script from the cloned iosched directory."
  exit 1
fi

echo "Installing git commit-message hook"
echo
curl -sSLo "${GIT_DIR}/hooks/commit-msg" \
    "https://gerrit-review.googlesource.com/tools/hooks/commit-msg" \
  && chmod +x "${GIT_DIR}/hooks/commit-msg"

echo "Installing git pre-push hook"
echo
cp "${GIT_ROOT}/tools/pre-push" "${GIT_DIR}/hooks/pre-push" \
  && chmod +x "${GIT_DIR}/hooks/pre-push"

cat <<-EOF
Please import the code style settings in Android Studio:
  * open Settings -> Editor -> Code Style
  * click the gear icon and select "Import Scheme..."
  * find the file ${GIT_ROOT}/tools/iosched-codestyle.xml

Additionally, checking the following settings helps avoid miscellaneous issues:
  * Settings -> Editor -> General -> Strip trailing spaces on Save
  * Settings -> Editor -> General -> Ensure line feed at end of file on Save
  * Settings -> Editor -> General -> Auto Import -> Optimize imports on the fly
EOF
