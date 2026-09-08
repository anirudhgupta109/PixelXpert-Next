#!/bin/bash

NEWVERNAME=$(cat version.properties | grep -i "VERSION_NAME" | cut -d = -f 2)

echo "**$NEWVERNAME**  " > newChangeLog.md
cat changeLog.md >> newChangeLog.md
echo "  " >> newChangeLog.md
cat CanaryChangelog.md >> newChangeLog.md
mv  newChangeLog.md CanaryChangelog.md

echo "$NEWVERNAME released in canary channel" > telegram.msg
echo "" >> telegram.msg
echo "Changelog:" >> telegram.msg
cat changeLog.md >> telegram.msg
echo "" >> telegram.msg
echo "URL: https://github.com/anirudhgupta109/PixelXpert-Next/releases/tag/canary_builds" >> telegram.msg
echo 'TMessage<<EOF' >> $GITHUB_ENV
cat telegram.msg >> $GITHUB_ENV
echo 'EOF' >> $GITHUB_ENV