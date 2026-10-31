#!/usr/bin/env bash
# チェックアウト済みの main に PR をマージする。
# refs/pull/<番号>/merge は main が進んでも古いままのことがあるため、ここでマージする。
set -euo pipefail

pr_number="$1"

git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
git fetch --no-tags "https://github.com/${GITHUB_REPOSITORY}.git" "refs/pull/${pr_number}/head"

if ! git merge --no-edit FETCH_HEAD; then
  echo "::error::PR #${pr_number} has conflicts with main. Resolve them and run this workflow again."
  exit 1
fi
