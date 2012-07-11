#!/bin/sh
if [[ -z $ADB ]]; then ADB=adb; fi

MAC_UNAME="Darwin"

if [[ "`uname`" == ${MAC_UNAME} ]]; then
  DATE_FORMAT="%Y-%m-%dT%H:%M:%S"
else
  DATE_FORMAT="%Y-%m-%d %H:%M:%S"
fi
if [ -z "$1" ]; then
  NOW_DATE=$(date "+${DATE_FORMAT}")
  echo Please provide a mock time in the format \"${NOW_DATE}\" or \"d\" to delete the mock time. >&2
  exit
fi

TEMP_FILE=$(mktemp -t iosched_mock_time.XXXXXXXX)
MOCK_TIME_STR="$1"

if [[ "d" == "${MOCK_TIME_STR}" ]]; then
  echo Deleting mock time... >&2
  ./kill_process.sh
  $ADB shell rm /data/data/com.google.android.apps.iosched/shared_prefs/mock_data.xml
  exit
fi

if [[ "`uname`" == ${MAC_UNAME} ]]; then
  MOCK_TIME_MSEC=$(date -j -f ${DATE_FORMAT} ${MOCK_TIME_STR} "+%s")000
else
  MOCK_TIME_MSEC=$(date -d "${MOCK_TIME_STR}" +%s)000
fi

echo Setting mock time to "${MOCK_TIME_STR}" == "${MOCK_TIME_MSEC}" ... >&2
cat >${TEMP_FILE}<<EOT
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
  <long name="mock_current_time" value="${MOCK_TIME_MSEC}"/>
</map>
EOT

./kill_process.sh
$ADB push ${TEMP_FILE} /data/data/com.google.android.apps.iosched/shared_prefs/mock_data.xml
