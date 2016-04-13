#!/bin/bash
# Generate a list of all Analytics calls.
# Used for generation of analytics-report.txt.


#First some intro text.

echo -e "Below is a report of all the Analytics data gathered in iosched. It
includes the approximate file name, line number, what event is being tracked,
and what data (other than the name of the event being tracked) is uploaded. This
report should be updated whenever the tracking code is updated, but feel free
to run it yourself from the Google IO App's source root using the following
command:

(in source root) $ docs/analytics-report.sh .--------------------------"


#Dump all analytics Screen Views / Events (Search by use of ANALYTICS) to stdout.

grep -r -n -E -A1 "ANALYTICS SCREEN|ANALYTICS EVENT" --include="*.java" . |
sed "s/\(:[[:digit:]]\+\):/\1\n/g" |  #Separate filename out into separate line
sed "s/.*-[[:digit:]]\+-//g" | #remove extra references to filename
sed "s/\(\s\)*\/\///g" #remove leading whitespace / comment formatting.
