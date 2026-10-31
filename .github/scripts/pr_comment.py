#!/usr/bin/env python3
"""search-compare の結果を PR にコメントする。コメントは PR ごとに 1 件で、最新の結果で更新する。

PR のコードを動かさないジョブから実行する。
"""
import os
import subprocess
import sys

MARKER = "<!-- search-compare -->"
SHORT_SHA_LENGTH = 7


def render_comment(counts, mismatches, run_url, commit):
    details = f"詳細: [実行結果]({run_url})"
    if not counts:
        return f"{MARKER}\n⚠️ **search-compare: 比較を実行できませんでした**\n\n{details}\n"

    names = [name for name in mismatches.split(",") if name]
    mark = "❌" if names else "✅"
    lines = [MARKER, f"{mark} **search-compare: {counts}**", ""]
    if names:
        lines += ["不一致のケース:", *[f"- {name}" for name in names], ""]
    lines += [details, f"比較したコミット: `{commit[:SHORT_SHA_LENGTH]}`"]
    return "\n".join(lines) + "\n"


def upsert_comment(repository, pr_number, body, run):
    comments_path = f"repos/{repository}/issues/{pr_number}/comments"
    query = f'.[] | select(.body | startswith("{MARKER}")) | .id'
    existing = run(["gh", "api", "--paginate", comments_path, "--jq", query]).split()

    if existing:
        path = f"repos/{repository}/issues/comments/{existing[0]}"
        run(["gh", "api", "--method", "PATCH", path, "-f", f"body={body}"])
    else:
        run(["gh", "api", "--method", "POST", comments_path, "-f", f"body={body}"])


def run_command(args):
    return subprocess.run(args, check=True, capture_output=True, text=True).stdout


def main():
    body = render_comment(
        os.environ.get("COUNTS", ""), os.environ.get("MISMATCHES", ""), os.environ["RUN_URL"], os.environ["COMMIT"]
    )
    upsert_comment(os.environ["GITHUB_REPOSITORY"], os.environ["PR_NUMBER"], body, run_command)
    return 0


if __name__ == "__main__":
    sys.exit(main())
