#!/bin/bash
set -eo pipefail

> changeLog.md
> Tchangelog.htm

LASTUPDATE=$(git log --grep="^Version update: Release" -n 1 --format="%H" 2>/dev/null || true)
RANGE="${LASTUPDATE:+$LASTUPDATE..}HEAD"

# 1. Extract explicit CHANGELOG lines across all commits
git log "$RANGE" --format="%B" 2>/dev/null | grep -i "^[[:space:]]*CHANGELOG:[[:space:]]*" | sed -E 's/^[[:space:]]*CHANGELOG:[[:space:]]*//I' | sed 's/^/- /; s/$/  /' > changeLog.md || true

# 2. If none, extract clean commit subjects
if [ ! -s changeLog.md ]; then
  git log "$RANGE" --no-merges --format="%s" 2>/dev/null | grep -vEi "^(New Crowdin|Version update:|canary release|Update edit2MakeNewCanary|Update CanaryChangelog\.md|Add archived notice|ci:|chore:|build:|test:|docs:|Stable release|stable-)" | sed -E 's/^[a-zA-Z]+(\([^)]+\))?:[[:space:]]*//' | sed 's/^/- /; s/$/  /' > changeLog.md || true
fi

# 3. Ultimate fallback
if [ ! -s changeLog.md ]; then
  echo "- Bug fixes and improvements  " > changeLog.md
fi
