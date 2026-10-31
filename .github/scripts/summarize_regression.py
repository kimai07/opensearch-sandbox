#!/usr/bin/env python3
"""リグレッションテストの結果を集計し、Job Summary に表を出力する。"""
import argparse
import json
import os
import pathlib
import sys

import search_report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", required=True, type=pathlib.Path)
    parser.add_argument("--cases", required=True, type=pathlib.Path)
    parser.add_argument("--environment", default="docker")
    parser.add_argument("--host", default="localhost:9200")
    args = parser.parse_args()

    if not args.report.exists():
        print(f"Report not found: {args.report}", file=sys.stderr)
        return 1

    cases = json.loads(args.cases.read_text(encoding="utf-8"))
    results = search_report.read_regression_results(args.report, cases)
    print(search_report.summary_line(results))
    print(search_report.mismatch_line(results))
    markdown = search_report.render_regression_markdown(results, args.environment, args.host)
    search_report.publish(markdown, results, os.environ)
    return 0


if __name__ == "__main__":
    sys.exit(main())
